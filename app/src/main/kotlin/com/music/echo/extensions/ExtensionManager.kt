package com.music.echo.extensions

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.iad1tya.LocalDatabase
import echo.music.iad1tya.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

@Serializable
data class AddonEndpoint(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true
)

@Serializable
data class AddonSearchResponse(
    val tracks: List<AddonTrack> = emptyList()
)

@Serializable
data class AddonTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: Int? = null,
    val artworkURL: String? = null,
    val albumArtworkURL: String? = null
)

@Serializable
data class AddonStreamResponse(
    val url: String = "",
    val format: String = "",
    val quality: String = "",
    val streamQuality: String = "",
    val audioQuality: String = "",
    val codec: String? = null
)

@Singleton
class ExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val httpClient = OkHttpClient()
    private val ADDONS_KEY = stringPreferencesKey("extensions_addons_list")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAddons(): List<AddonEndpoint> {
        val jsonStr = context.dataStore.data.map { it[ADDONS_KEY] }.first() ?: "[]"
        return try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addAddon(endpoint: AddonEndpoint) {
        val current = getAddons().toMutableList()
        current.add(endpoint)
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }
    
    suspend fun toggleAddon(id: String, enabled: Boolean) {
        val current = getAddons().map { if (it.id == id) it.copy(enabled = enabled) else it }
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }

    suspend fun removeAddon(id: String) {
        val current = getAddons().filter { it.id != id }
        context.dataStore.edit {
            it[ADDONS_KEY] = json.encodeToString(current)
        }
    }

    private fun baseUrlFor(addon: AddonEndpoint): String =
        if (addon.url.startsWith("http")) addon.url else "https://" + addon.url

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    /**
     * Robust addon-track parsing. The public track id itself is base64url and
     * contains `_`/`-`, so a naive split("_") breaks. Match by addon id prefix.
     */
    private fun findAddonForFullId(
        fullId: String,
        addons: List<AddonEndpoint>
    ): Pair<AddonEndpoint, String>? {
        if (!fullId.startsWith("addon_")) return null
        val rest = fullId.removePrefix("addon_")
        // Longest addon-id match first so similar prefixes don't collide.
        for (addon in addons.sortedByDescending { it.id.length }) {
            if (rest == addon.id) return addon to ""
            if (rest.startsWith(addon.id + "_")) {
                return addon to rest.removePrefix(addon.id + "_")
            }
        }
        return null
    }

    private fun normalizeForMatch(value: String): String =
        value.lowercase()
            .replace(Regex("[\\p{Punct}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Strict replacement match. The old code returned the first addon result
     * when nothing matched, so track N+1 played track N audio. Now null means
     * "no usable replacement" and the caller falls back to YouTube.
     *
     * Comparison is token based in both directions so catalogue naming variants
     * still match ("Portami" vs "Portami via / Fabrizio Moro", "Live", deluxe
     * suffixes, feature credits), while a genuinely different song does not:
     * every token of the shorter side must be covered by the longer one.
     * Duration is only used to break ties, never to reject.
     */
    private fun selectBestMatch(
        tracks: List<AddonTrack>,
        title: String,
        artist: String,
        durationMs: Long?
    ): AddonTrack? {
        if (tracks.isEmpty()) return null
        val wantTitle = normalizeForMatch(title)
        if (wantTitle.isBlank()) return null
        val wantTokens = wantTitle.split(" ").filter { it.isNotBlank() }
        val artistTokens = normalizeForMatch(artist)
            .split(" ", ",", "&", "/", ";", "+")
            .map { it.trim() }
            .filter { it.length >= 2 }
            .toSet()
        var best: AddonTrack? = null
        var bestScore = Int.MIN_VALUE
        for (track in tracks) {
            if (track.id.isBlank() || track.title.isBlank()) continue
            val gotTitle = normalizeForMatch(track.title)
            if (gotTitle.isBlank()) continue
            val gotTokens = gotTitle.split(" ").filter { it.isNotBlank() }
            val covered =
                gotTokens.count { got -> wantTokens.any { want -> got.contains(want) || want.contains(got) } }
            val total = maxOf(gotTokens.size, wantTokens.size)
            // Require every token of the shorter side to be covered, and at
            // least half of the longer side so shared words alone don't qualify.
            val coverage = covered.toFloat() / total
            if (coverage < 0.5f) continue
            // Full coverage of the catalogue title is the strongest signal.
            var score = (coverage * 100).toInt()
            val gotArtist = normalizeForMatch(track.artist)
            if (artistTokens.isNotEmpty() && gotArtist.isNotBlank()) {
                if (artistTokens.any { gotArtist.contains(it) }) score += 40
            }
            if (durationMs != null && durationMs > 0 && track.duration != null && track.duration > 0) {
                val diffMs = kotlin.math.abs(track.duration * 1000L - durationMs)
                if (diffMs <= 8_000L) score += 10
            }
            if (score > bestScore) {
                bestScore = score
                best = track
            }
        }
        return best
    }

    suspend fun search(query: String): List<SongItem> = withContext(Dispatchers.IO) {
        val addons = getAddons().filter { it.enabled }
        val results = mutableListOf<SongItem>()
        
        for (addon in addons) {
            try {
                val baseUrl = baseUrlFor(addon)
                val searchUrl = "${baseUrl.removeSuffix("/")}/search?q=${encodeQuery(query)}"
                val request = Request.Builder().url(searchUrl).build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val addonResponse: AddonSearchResponse = json.decodeFromString(body)
                    val addonResults = addonResponse.tracks
                    results.addAll(addonResults.map { 
                        SongItem(
                            id = "addon_${addon.id}_${it.id}",
                            title = it.title,
                            artists = listOf(Artist(name = it.artist, id = null)),
                            thumbnail = it.artworkURL ?: it.albumArtworkURL ?: "",
                            duration = it.duration
                        )
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        results
    }

    suspend fun getStreamUrl(fullId: String): String? = withContext(Dispatchers.IO) {
        // fullId format: addon_{addonId}_{trackId}; trackId itself may contain "_".
        val addons = getAddons().filter { it.enabled }
        val (addon, trackId) = findAddonForFullId(fullId, addons) ?: return@withContext null
        if (trackId.isBlank()) return@withContext null
        
        try {
            val baseUrl = baseUrlFor(addon)
            // BitChord contract is /stream/:id (path param), not /stream?id=.
            // Encoded as a single path segment so ? # / in ids can't split the URL.
            val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream/${Uri.encode(trackId)}?quality=LOSSLESS"
            val request = Request.Builder().url(streamUrlReq).build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            
            if (response.isSuccessful && !body.isNullOrEmpty()) {
                val streamRes: AddonStreamResponse = json.decodeFromString(body)
                return@withContext streamRes.url
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun resolveStream(title: String?, artist: String?, duration: Long?, videoId: String): AddonStreamResponse? = withContext(Dispatchers.IO) {
        if (videoId.startsWith("addon_")) {
            val addons = getAddons().filter { it.enabled }
            val (targetAddon, trackId) = findAddonForFullId(videoId, addons)
                ?: return@withContext null
            if (trackId.isBlank()) return@withContext null
            val baseUrl = baseUrlFor(targetAddon)
            val streamUrlReq = "${baseUrl.removeSuffix("/")}/stream/${Uri.encode(trackId)}?quality=LOSSLESS"
            val streamReq = Request.Builder().url(streamUrlReq).build()
            val streamRes = httpClient.newCall(streamReq).execute()
            val streamBody = streamRes.body?.string()
            if (streamRes.isSuccessful && !streamBody.isNullOrEmpty()) {
                return@withContext json.decodeFromString<AddonStreamResponse>(streamBody)
            }
            return@withContext null
        }
        if (title == null || artist == null) return@withContext null
        val addons = getAddons().filter { it.enabled }
        if (addons.isEmpty()) return@withContext null
        
        val query = "$title $artist"
        for (addon in addons) {
            val baseUrl = baseUrlFor(addon)
            // The BitChord contract also exposes isolated source prefixes (/a, /b, /c).
            // Those can 502 when one upstream is cold or down, while the root
            // provider races all backends, so fall back to it before giving up
            // and letting playback drop to YouTube (Opus).
            val searchBases = buildList {
                add(baseUrl)
                BASE_ALTERNATIVES.forEach { alternative ->
                    val candidate = baseUrl.substringBefore("/a").substringBefore("/b")
                        .substringBefore("/c").removeSuffix("/") + alternative
                    if (candidate != baseUrl && candidate !in this) add(candidate)
                }
            }
            for (searchBase in searchBases) {
                try {
                    android.util.Log.e("ExtensionManager", "Searching addon $searchBase for $title $artist")
                    val searchUrl = "${searchBase.removeSuffix("/")}/search?q=${encodeQuery(query)}"
                    val request = Request.Builder().url(searchUrl).build()
                    val response = httpClient.newCall(request).execute()
                    val body = response.body?.string()
                    
                    if (response.isSuccessful && !body.isNullOrEmpty()) {
                        val addonResponse: AddonSearchResponse = json.decodeFromString(body)
                        val addonResults = addonResponse.tracks
                        // Strict match: never fall back to an unrelated first result.
                        // Returning null lets the player use YouTube instead of wrong audio.
                        val match = selectBestMatch(addonResults, title, artist, duration)
                        if (match == null) {
                            android.util.Log.e("ExtensionManager", "No reliable addon match for $title - $artist on $searchBase")
                            continue
                        }
                        
                        android.util.Log.e("ExtensionManager", "Matched track: ${match.title} by ${match.artist}. Fetching stream...")
                        val streamUrlReq = "${searchBase.removeSuffix("/")}/stream/${Uri.encode(match.id)}?quality=LOSSLESS"
                        val streamReq = Request.Builder().url(streamUrlReq).build()
                        val streamRes = httpClient.newCall(streamReq).execute()
                        val streamBody = streamRes.body?.string()
                        if (streamRes.isSuccessful && !streamBody.isNullOrEmpty()) {
                            android.util.Log.e("ExtensionManager", "Stream response: $streamBody")
                            val streamData: AddonStreamResponse = json.decodeFromString(streamBody)
                            return@withContext streamData
                        } else {
                            android.util.Log.e("ExtensionManager", "Stream request failed: ${streamRes.code} ${streamRes.message}")
                        }
                    } else {
                        android.util.Log.e("ExtensionManager", "Search failed: ${response.code} from $searchBase")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ExtensionManager", "Addon request failed for $searchBase", e)
                }
            }
        }
        null
    }

    private companion object {
        /** Isolated BitChord source prefixes tried when the configured one fails. */
        val BASE_ALTERNATIVES = listOf("", "/c", "/a")
    }
}
