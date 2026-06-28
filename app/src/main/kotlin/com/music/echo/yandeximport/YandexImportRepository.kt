/*
 * EchoMusic (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package iad1tya.echo.music.yandeximport

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.PlaylistEntity
import iad1tya.echo.music.db.entities.PlaylistSongMap
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.spotify.SpotifyMapper
import iad1tya.echo.music.utils.reportException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YandexImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val client = OkHttpClient()
    private val mapperMutex = Mutex()

    suspend fun fetchYandexPlaylist(url: String): YandexPlaylist? =
        withContext(Dispatchers.IO) {
            try {
                // Ensure it is a full URL
                val targetUrl = if (url.contains("music.yandex.ru")) url else "https://music.yandex.ru/playlists/$url"
                
                val pageRequest = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .build()
                
                val html = client.newCall(pageRequest).execute().use { it.body?.string() }
                if (html.isNullOrBlank()) return@withContext null
                
                var title = "Без названия"
                val titleRegex = Regex("<title>(.*?)</title>")
                val titleMatch = titleRegex.find(html)
                if (titleMatch != null && !titleMatch.groupValues[1].contains("Яндекс", ignoreCase = true)) {
                    title = titleMatch.groupValues[1].replace(" — Яндекс Музыка", "").trim()
                }
                
                val htmlReplaced = html.replace("\\u002F", "/")
                
                // Regex 1: for system / lk.UUID
                val trackIdsRegex1 = Regex("\"path\":\"/playlist/items/\\d+\",\"value\":\\{\"id\":\"(\\d+)\"")
                var matchResults = trackIdsRegex1.findAll(htmlReplaced).map { it.groupValues[1] }.toList()
                
                // Regex 2: for normal users playlists
                if (matchResults.isEmpty()) {
                    val trackIdsRegex2 = Regex("\\\\\"id\\\\\":(\\d+),\\\\\"albumId\\\\\":")
                    matchResults = trackIdsRegex2.findAll(html).map { it.groupValues[1] }.toList()
                }
                
                val uniqueIds = matchResults.distinct()
                if (uniqueIds.isEmpty()) return@withContext null
                
                val tracks = mutableListOf<YandexTrack>()
                
                // Batch requests (chunks of 100)
                val chunkedIds = uniqueIds.chunked(100)
                for (chunk in chunkedIds) {
                    val idsStr = chunk.joinToString(",")
                    val apiUrl = "https://api.music.yandex.net/tracks?trackIds=$idsStr"
                    
                    val apiRequest = Request.Builder()
                        .url(apiUrl)
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                        
                    val apiResponse = client.newCall(apiRequest).execute().use { it.body?.string() }
                    if (!apiResponse.isNullOrBlank()) {
                        val json = JSONObject(apiResponse)
                        val resultArray = json.optJSONArray("result")
                        if (resultArray != null) {
                            for (i in 0 until resultArray.length()) {
                                val trackData = resultArray.getJSONObject(i)
                                val trackTitle = trackData.optString("title", "Unknown")
                                val version = trackData.optString("version", "")
                                val fullTitle = if (version.isNotEmpty()) "$trackTitle ($version)" else trackTitle
                                
                                val artistsArray = trackData.optJSONArray("artists")
                                val artistsList = mutableListOf<String>()
                                if (artistsArray != null) {
                                    for (j in 0 until artistsArray.length()) {
                                        artistsList.add(artistsArray.getJSONObject(j).optString("name", "Unknown"))
                                    }
                                }
                                
                                val durationMs = trackData.optLong("durationMs", 0L)
                                tracks.add(YandexTrack(fullTitle, artistsList.joinToString(", "), durationMs))
                            }
                        }
                    }
                }
                
                return@withContext YandexPlaylist(title, tracks)
            } catch (e: Exception) {
                reportException(e)
                return@withContext null
            }
        }

    suspend fun importFromUrl(url: String, onProgress: (Int, Int) -> Unit): Boolean {
        // Now handles any URL format natively by delegating entirely to HTML parsing
        if (!url.contains("music.yandex.ru")) return false
        return importPlaylist(url, onProgress)
    }

    suspend fun importPlaylist(
        url: String,
        onProgress: (Int, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val yandexPlaylist = fetchYandexPlaylist(url) ?: return@withContext false
        val matchedTracks = matchTracks(yandexPlaylist.tracks, onProgress)
        
        // Generate a simple ID hash based on URL
        val localPlaylistId = "YANDEX_PLAYLIST_${url.hashCode()}"
        mirrorPlaylist(localPlaylistId, yandexPlaylist.title, matchedTracks.map { it.metadata })
        return@withContext true
    }

    private suspend fun matchTracks(
        tracks: List<YandexTrack>,
        onProgress: (Int, Int) -> Unit
    ): List<MatchedTrack> = coroutineScope {
        val semaphore = Semaphore(4)
        val completed = AtomicInteger(0)

        tracks.mapIndexed { index, track ->
            async {
                semaphore.withPermit {
                    val matched = matchTrack(track, index)
                    val completedCount = completed.incrementAndGet()
                    onProgress(completedCount, tracks.size)
                    matched
                }
            }
        }.awaitAll().filterNotNull().sortedBy { it.index }
    }

    private suspend fun matchTrack(track: YandexTrack, index: Int): MatchedTrack? {
        val query = "${track.artists} ${track.title}"
        val searchResult = YouTube.search(query = query, filter = YouTube.SearchFilter.FILTER_SONG)
            .getOrElse { error ->
                if (error is CancellationException) throw error
                return null
            }
            
        val candidates = searchResult.items.filterIsInstance<SongItem>().distinctBy { it.id }
        
        val best = mapperMutex.withLock {
            candidates.maxByOrNull { candidate ->
                SpotifyMapper.matchScore(
                    spotifyTitle = track.title,
                    spotifyArtist = track.artists,
                    spotifyDurationMs = track.durationMs.toInt(),
                    candidateTitle = candidate.title,
                    candidateArtist = candidate.artists.joinToString(" ") { it.name },
                    candidateDurationSec = candidate.duration,
                )
            }
        } ?: return null

        return MatchedTrack(index = index, metadata = best.toMediaMetadata())
    }

    private suspend fun mirrorPlaylist(
        localPlaylistId: String,
        title: String,
        tracks: List<MediaMetadata>,
    ) {
        database.withTransaction {
            val existing = getPlaylistById(localPlaylistId)
            val now = LocalDateTime.now()
            val entity = existing?.playlist?.copy(
                name = title,
                lastUpdateTime = now,
            ) ?: PlaylistEntity(
                id = localPlaylistId,
                name = title,
                bookmarkedAt = now,
                lastUpdateTime = now,
                thumbnailUrl = null,
                isEditable = true,
            )

            if (existing == null) {
                insert(entity)
            } else {
                update(entity)
            }

            tracks.forEach { metadata -> insert(metadata) }

            clearPlaylist(localPlaylistId)
            tracks.forEachIndexed { index, metadata ->
                insert(
                    PlaylistSongMap(
                        playlistId = localPlaylistId,
                        songId = metadata.id,
                        position = index,
                        setVideoId = metadata.setVideoId,
                    ),
                )
            }
            update(entity.copy(lastUpdateTime = now))
        }
    }

    private data class MatchedTrack(val index: Int, val metadata: MediaMetadata)
}

data class YandexPlaylist(
    val title: String,
    val tracks: List<YandexTrack>
)

data class YandexTrack(
    val title: String,
    val artists: String,
    val durationMs: Long
)
