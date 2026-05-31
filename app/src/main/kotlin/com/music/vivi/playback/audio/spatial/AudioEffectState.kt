package com.music.vivi.playback.audio.spatial

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AudioEffectState(
    val eqEnabled: Boolean = true,
    val eqBands: List<Float> = List(10) { 0f },
    val bassBoost: Float = 0f,
    val virtualizer: Float = 0f,
    val spatialEnabled: Boolean = false,
    val crossfeedEnabled: Boolean = true,
    val limiterMakeupGain: Float = 0f,
    val limiterThresholdDb: Float = -0.1f,
    val limiterRatio: Float = 4.0f,
    val limiterAttackMs: Float = 5.0f,
    val limiterReleaseMs: Float = 100.0f
) {
    val isEqEnabled get() = eqEnabled
    val safeEqBands: List<Float> get() = eqBands
    val safeBassBoost get() = bassBoost
    val safeVirtualizer get() = virtualizer
    val isSpatialEnabled get() = spatialEnabled
    val isCrossfeedEnabled get() = crossfeedEnabled
    val safeLimiterMakeupGain get() = limiterMakeupGain
    val safeLimiterThresholdDb get() = limiterThresholdDb
    val safeLimiterRatio get() = limiterRatio
    val safeLimiterAttackMs get() = limiterAttackMs
    val safeLimiterReleaseMs get() = limiterReleaseMs

    fun toJson(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun fromJson(json: String): AudioEffectState {
            if (json.isBlank()) return AudioEffectState()
            return try {
                Json.decodeFromString<AudioEffectState>(json)
            } catch (e: Exception) {
                AudioEffectState()
            }
        }
    }
}

data class SignalStats(
    val peakLevel: Float,
    val rmsLevel: Float
)
