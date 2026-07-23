package iad1tya.echo.music.utils

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

object JioSaavnFallback {
    private val client = OkHttpClient()

    private fun getDomain(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "default_id"
        val hash = androidId.hashCode()
        return if (hash % 2 == 0) {
            "jiosaavn-api.pc-adityadav9532.workers.dev"
        } else {
            "jiosaavn-api.mac-adityadav9532.workers.dev"
        }
    }

    suspend fun resolveAgeRestrictedSong(context: Context, title: String, artist: String): String? = withContext(Dispatchers.IO) {
        suspend fun searchSaavn(searchQuery: String): String? {
            try {
                val domain = getDomain(context)
                val url = "https://$domain/api/search/songs?query=${java.net.URLEncoder.encode(searchQuery, "UTF-8")}"
                Timber.tag("JioSaavnFallback").d("Requesting URL: $url")
                val request = Request.Builder().url(url).build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Timber.tag("JioSaavnFallback").e("JioSaavn request failed: ${response.code}")
                    return null
                }

                val bodyString = response.body?.string() ?: return null
                Timber.tag("JioSaavnFallback").d("Response: $bodyString")
                val json = JSONObject(bodyString)

                if (json.getBoolean("success")) {
                    val results = json.getJSONObject("data").getJSONArray("results")
                    if (results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val downloadUrls = firstResult.getJSONArray("downloadUrl")

                        if (downloadUrls.length() > 0) {
                            val bestQualityUrlObj = downloadUrls.getJSONObject(downloadUrls.length() - 1)
                            return bestQualityUrlObj.getString("url")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error resolving JioSaavn fallback")
            }
            return null
        }

        val fullQuery = "$title $artist".trim()
        var result = searchSaavn(fullQuery)
        
        if (result == null && artist.isNotEmpty()) {
            Timber.tag("JioSaavnFallback").d("Search with artist failed, trying title only: $title")
            result = searchSaavn(title)
        }
        
        return@withContext result
    }
}
