package iad1tya.echo.music.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object OdesliRepository {
    private val client = OkHttpClient()

    /**
     * Fetches the Odesli/Songlink page URL for the given YouTube Music video ID or URL.
     * Uses the explicit platform/type/id lookup which gives the best cross-platform matching.
     * Returns the universal "all platforms" page URL, or null on failure.
     * Rate limit: 10 requests/min unauthenticated.
     */
    suspend fun getPageUrl(songUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            // Extract the raw video ID from any YouTube / YouTube Music URL form,
            // e.g. https://music.youtube.com/watch?v=abc123  or  https://youtu.be/abc123
            val videoId = extractYouTubeVideoId(songUrl)

            val apiUrl = if (videoId != null) {
                // Use the explicit platform/type/id form — gives far better cross-platform
                // matching than passing the full URL through url=...
                "https://api.song.link/v1-alpha.1/links" +
                    "?platform=youtubeMusic&type=song&id=${URLEncoder.encode(videoId, "UTF-8")}" +
                    "&songIfSingle=true"
            } else {
                // Fallback: pass the raw URL for non-YTM sources
                val encodedUrl = URLEncoder.encode(songUrl, "UTF-8")
                "https://api.song.link/v1-alpha.1/links?url=$encodedUrl&songIfSingle=true"
            }

            val request = Request.Builder()
                .url(apiUrl)
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val pageUrl = json.optString("pageUrl")
                if (pageUrl.isNotEmpty()) pageUrl else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Extracts the 11-character YouTube video ID from various YouTube / YouTube Music URL formats. */
    private fun extractYouTubeVideoId(url: String): String? {
        // music.youtube.com/watch?v=VIDEO_ID or youtube.com/watch?v=VIDEO_ID
        val watchRegex = Regex("[?&]v=([a-zA-Z0-9_-]{11})")
        watchRegex.find(url)?.groupValues?.get(1)?.let { return it }
        // youtu.be/VIDEO_ID
        val shortRegex = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})")
        shortRegex.find(url)?.groupValues?.get(1)?.let { return it }
        return null
    }
}
