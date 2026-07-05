package com.music.jiosaavn

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs

object DeviceRouter {

    private const val PREFS_NAME = "echo"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_CACHED_SERVERS = "cached_saavn_servers"
    private const val REMOTE_CONFIG_URL = "https://echomusic.fun/saavn.json"

    private val SERVERS = listOf(
        "https://saavn.nullmusic.fun",
        "https://jiosaavn-api.pc-adityadav9532.workers.dev",
        "https://jiosaavn-api.mac-adityadav9532.workers.dev"
    )

    private var deviceId: String? = null
    private var assignedServerIndex: Int = 0
    private var currentSessionServerIndex: Int = 0
    private var isInitialized = false

    private val jsonConfig = Json { ignoreUnknownKeys = true }
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    fun init(context: Context) {
        if (isInitialized) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val id = prefs.getString(KEY_DEVICE_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            newId
        }
        
        deviceId = id

        val cachedServersJson = prefs.getString(KEY_CACHED_SERVERS, null)
        if (cachedServersJson != null) {
            try {
                val config = jsonConfig.decodeFromString<RemoteServerConfig>(cachedServersJson)
                if (config.servers.isNotEmpty()) {
                    activeServers = config.servers
                }
            } catch (e: Exception) {
                // Ignore parse errors, stick to default
            }
        }

        assignServer()
        isInitialized = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: RemoteServerConfig = httpClient.get(REMOTE_CONFIG_URL).body()
                if (response.servers.isNotEmpty() && response.servers != activeServers) {
                    val newJson = jsonConfig.encodeToString(RemoteServerConfig.serializer(), response)
                    prefs.edit().putString(KEY_CACHED_SERVERS, newJson).apply()
                    activeServers = response.servers
                    assignServer()
                }
            } catch (e: Exception) {
                // Network error, fail silently
            }
        }
    }

    private fun assignServer() {
        val id = deviceId ?: return
        if (activeServers.isEmpty()) return
        assignedServerIndex = abs(id.hashCode()) % activeServers.size
        currentSessionServerIndex = assignedServerIndex
    }

    fun getDeviceId(): String {
        check(isInitialized) { "DeviceRouter must be initialized first" }
        return deviceId!!
    }

    /**
     * Returns the full server URL for this device based on the current session's fallback state.
     */
    fun getCurrentServer(): String {
        check(isInitialized) { "DeviceRouter must be initialized first" }
        check(activeServers.isNotEmpty()) { "No JioSaavn servers configured" }
        return activeServers[currentSessionServerIndex]
    }

    /**
     * Returns the 0-based index of the currently active server.
     */
    fun getCurrentServerIndex(): Int {
        check(isInitialized) { "DeviceRouter must be initialized first" }
        check(activeServers.isNotEmpty()) { "No JioSaavn servers configured" }
        return currentSessionServerIndex
    }

    /**
     * Checks whether there is at least one remote server configured.
     */
    fun hasServers(): Boolean {
        check(isInitialized) { "DeviceRouter must be initialized first" }
        return activeServers.isNotEmpty()
    }

    /**
     * Fallback to the next server in the list for the duration of this session.
     */
    fun fallbackToNextServer() {
        check(isInitialized) { "DeviceRouter must be initialized first" }
        if (activeServers.isEmpty()) return
        currentSessionServerIndex = (currentSessionServerIndex + 1) % activeServers.size
    }
}
