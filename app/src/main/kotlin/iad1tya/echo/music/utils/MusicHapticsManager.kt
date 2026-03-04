package iad1tya.echo.music.utils

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class MusicHapticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MusicHapticsManager"
        // Minimum time between vibrations to avoid over-buzzing
        private const val MIN_BEAT_INTERVAL_MS = 80L
        // RMS level (0..1) that must be exceeded to trigger a vibration
        private const val AMPLITUDE_THRESHOLD = 0.25f
        private const val VIBRATION_DURATION_MS = 20L
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService<VibratorManager>()?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var visualizer: Visualizer? = null
    @Volatile private var isEnabled = false
    private var lastBeatTime = 0L
    private var previousAmplitude = 0f

    /**
     * Attach to [audioSessionId] and start responding to actual audio amplitude.
     * Must be called from the main thread (Visualizer requirement).
     */
    fun start(audioSessionId: Int) {
        if (vibrator == null || vibrator.hasVibrator().not()) {
            Log.w(TAG, "No vibrator available")
            return
        }
        if (audioSessionId <= 0) {
            Log.w(TAG, "Invalid audioSessionId: $audioSessionId, haptics unavailable")
            return
        }
        stop() // release any previous Visualizer
        isEnabled = true
        previousAmplitude = 0f
        lastBeatTime = 0L
        try {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val captureSize = captureSizeRange[0].coerceAtLeast(256)
                .coerceAtMost(captureSizeRange[1])
            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            vis: Visualizer,
                            waveform: ByteArray,
                            samplingRate: Int
                        ) {
                            if (!isEnabled) return
                            // PCM bytes are unsigned 0–255, centered at 128
                            var sum = 0L
                            for (b in waveform) {
                                val s = ((b.toInt() and 0xFF) - 128).toLong()
                                sum += s * s
                            }
                            val rms = sqrt(sum.toDouble() / waveform.size).toFloat()
                            // Max theoretical RMS ≈ 128; normalise to 0..1
                            val normalised = (rms / 128f).coerceIn(0f, 1f)
                            processAmplitude(normalised)
                        }
                        override fun onFftDataCapture(
                            vis: Visualizer,
                            fft: ByteArray,
                            samplingRate: Int
                        ) { /* unused */ }
                    },
                    // rate in mHz; request max supported up to 60 Hz
                    Visualizer.getMaxCaptureRate().coerceAtMost(60_000),
                    true,   // waveform
                    false   // fft
                )
                enabled = true
            }
            Log.d(TAG, "Visualizer started on session $audioSessionId, captureSize=$captureSize")
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer init failed: ${e.message}")
            isEnabled = false
        }
    }

    fun stop() {
        isEnabled = false
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) { }
        visualizer = null
        try { vibrator?.cancel() } catch (_: Exception) { }
    }

    private fun processAmplitude(normalizedAmplitude: Float) {
        val now = System.currentTimeMillis()
        if (now - lastBeatTime < MIN_BEAT_INTERVAL_MS) return

        // Fire on rising edge above threshold (simple onset detection)
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
