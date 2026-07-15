package iad1tya.echo.music.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@Serializable
data class LosslessIndex(
    val items: List<LosslessTrack> = emptyList()
)

@Serializable
data class LosslessTrack(
    val song: String,
    val artist: String,
    val url: String
)

object LosslessAPI {
    private val httpClient = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    
    private var cachedIndex: List<LosslessTrack>? = null
    private var lastFetchTime = 0L

    private suspend fun fetchMusicList(): List<LosslessTrack> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedIndex != null && (now - lastFetchTime) < 1000 * 60 * 60) {
            return@withContext cachedIndex!!
        }

        try {
            val request = Request.Builder()
                .url("https://lossless.echomusic.fun/music.json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val index = json.decodeFromString<LosslessIndex>(responseBody)
                    cachedIndex = index.items
                    lastFetchTime = now
                    return@withContext index.items
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch lossless music index")
        }
        
        return@withContext cachedIndex ?: emptyList()
    }

    suspend fun search(queryTitle: String, queryArtist: String): LosslessTrack? {
        val list = fetchMusicList()
        val titleTarget = queryTitle.trim().lowercase()
        val artistTarget = queryArtist.trim().lowercase()
        
        val track = list.find { track ->
            val trackTitle = track.song.trim().lowercase()
            val trackArtist = track.artist.trim().lowercase()
            
            trackTitle == titleTarget && (trackArtist.contains(artistTarget) || artistTarget.contains(trackArtist))
        }
        
        return track?.let {
            val resolvedUrl = it.url.replace(
                "https://lossless.echomusic.fun/Music/",
                "https://raw.githubusercontent.com/EchoMusicApp/Lossless/main/Music/"
            )
            it.copy(url = resolvedUrl)
        }
    }
}
