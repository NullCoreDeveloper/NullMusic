package com.music.jiosaavn

import kotlinx.serialization.Serializable

@Serializable
data class RemoteServerConfig(
    val version: Int,
    val servers: List<String>
)
