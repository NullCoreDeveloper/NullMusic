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

    suspend fun fetchYandexPlaylist(username: String, playlistId: String): YandexPlaylist? =
        withContext(Dispatchers.IO) {
            try {
                val pageUrl = "https://music.yandex.ru/users/$username/playlists/$playlistId"
                val apiUrl = "https://music.yandex.ru/handlers/playlist.jsx?owner=$username&kinds=$playlistId"
                
                // Fetch page to get cookies
                val pageRequest = Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/121.0.0.0")
                    .build()
                client.newCall(pageRequest).execute().use { }

                // Fetch API
                val apiRequest = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/121.0.0.0")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", pageUrl)
                    .build()
                    
                val responseBody = client.newCall(apiRequest).execute().use { it.body?.string() }
                if (responseBody.isNullOrBlank()) return@withContext null
                
                val json = JSONObject(responseBody)
                if (!json.has("playlist")) return@withContext null
                
                val playlistJson = json.getJSONObject("playlist")
                val title = playlistJson.optString("title", "Без названия")
                val tracksArray = playlistJson.optJSONArray("tracks") ?: return@withContext null
                
                val tracks = mutableListOf<YandexTrack>()
                for (i in 0 until tracksArray.length()) {
                    val trackItem = tracksArray.getJSONObject(i)
                    val trackData = if (trackItem.has("track")) trackItem.getJSONObject("track") else trackItem
                    
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
                
                return@withContext YandexPlaylist(title, tracks)
            } catch (e: Exception) {
                reportException(e)
                return@withContext null
            }
        }

    suspend fun importPlaylist(
        username: String,
        playlistId: String,
        onProgress: (Int, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val yandexPlaylist = fetchYandexPlaylist(username, playlistId) ?: return@withContext false
        val matchedTracks = matchTracks(yandexPlaylist.tracks, onProgress)
        
        val localPlaylistId = "YANDEX_PLAYLIST_${username}_$playlistId"
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
                    spotifyDurationMs = track.durationMs,
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
