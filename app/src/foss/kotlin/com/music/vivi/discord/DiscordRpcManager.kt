package com.music.vivi.discord

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DiscordUser(
    val id: String,
    val username: String,
    val name: String,
    val avatar: String?,
)

/**
 * Stub DiscordRpcManager for FOSS builds.
 * Discord RPC is not available without the Discord Partner SDK.
 */
object DiscordRpcManager {
    private val _accessTokenFlow = MutableStateFlow<String?>(null)
    val accessTokenFlow: StateFlow<String?> = _accessTokenFlow

    private val _connectionStatus = MutableStateFlow(Status.Disconnected)
    val connectionStatus: StateFlow<Status> = _connectionStatus

    private val _settingsChanged = MutableStateFlow(0)
    val settingsChanged: StateFlow<Int> = _settingsChanged

    fun notifySettingsChanged() {}

    enum class Status { Disconnected, Authorizing, Connected }

    enum class StatusType(val value: Int) {
        Online(0),
        Idle(3),
        Dnd(4),
    }

    @JvmStatic
    fun onNativeStatusChanged(statusCode: Int, ready: Boolean, authorized: Boolean) {}

    fun getAccessToken(): String? = null

    fun isInitialized(): Boolean = false
    fun isAuthorized(): Boolean = false
    fun isReady(): Boolean = false

    fun init(context: Context) {}
    fun authorize(onComplete: (Boolean) -> Unit) { onComplete(false) }
    fun fetchCurrentUser(token: String): DiscordUser? = null
    fun setActivity(activity: DiscordActivity) {}
    fun setOnlineStatus(status: StatusType) {}
    fun clear() {}
    fun reconnectWithToken(token: String) {}
    fun destroy() {}
    fun disconnect() {}
    fun logout() {}
}
