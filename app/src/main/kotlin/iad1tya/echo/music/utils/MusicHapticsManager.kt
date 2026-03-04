package iad1tya.echo.music.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicHapticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MusicHapticsManager"
        private const val MIN_BEAT_INTERVAL_MS = 80L
        private const val AMPLITUDE_THRESHOLD = 0.35f
        private const val VIBRATION_DURATION_MS = 25L
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService<VibratorManager>()?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var isEnabled = false
    private var lastBeatTime = 0L
    private var previousAmplitude = 0f
    private var scope: CoroutineScope? = null

    fun start() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.w(TAG, "No vibrator available")
            return
        }
        isEnabled = true
        previousAmplitude = 0f
        lastBeatTime = 0L
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    fun stop() {
        isEnabled = false
        scope?.cancel()
        scope = null
        vibrator?.cancel()
    }

    /**
     * Feed normalized amplitude (0.0f to 1.0f) from audio processor.
     * Call this periodically (e.g., every 50ms from player position polling).
     */
    fun onAmplitude(normalizedAmplitude: Float) {
        if (!isEnabled || vibrator == null) return

        val now = System.currentTimeMillis()
        if (now - lastBeatTime < MIN_BEAT_INTERVAL_MS) return

        // Simple beat detection: amplitude spike above threshold
        // and rising from previous sample
        val isRising = normalizedAmplitude > previousAmplitude
        val isAboveThreshold = normalizedAmplitude > AMPLITUDE_THRESHOLD
        previousAmplitude = normalizedAmplitude

        if (isRising && isAboveThreshold) {
            lastBeatTime = now
            vibrate(normalizedAmplitude)
        }
    }

    private fun vibrate(intensity: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = (intensity * 255).toInt().coerceIn(1, 255)
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(VIBRATION_DURATION_MS, amplitude)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_DURATION_MS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error: ${e.message}")
        }
    }

    fun isAvailable(): Boolean = vibrator?.hasVibrator() == true
}
