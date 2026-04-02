package iad1tya.echo.music.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import iad1tya.echo.music.constants.AudioArAutoCalibrateKey
import iad1tya.echo.music.constants.AudioArEnabledKey
import iad1tya.echo.music.constants.AudioArSensitivityKey
import iad1tya.echo.music.constants.AudioArCenterPointKey
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Manages spatial audio / Audio AR (Augmented Reality) features.
 * Tracks device orientation and applies soundstage rotation effects.
 */
class SpatialAudioManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val scope = CoroutineScope(Dispatchers.Default)

    // Sensor data
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3) // azimuth, pitch, roll

    // Calibration center point
    private var centerAzimuth = 0f
    private var centerPitch = 0f
    private var centerRoll = 0f

    // Current soundstage parameters
    var currentSoundstageRotation = 0f
        private set

    var currentSoundstageDepth = 1f
        private set

    private var isEnabled = false
    private var autoCalibrate = false
    private var sensitivity = 1f

    init {
        scope.launch {
            dataStore.data
                .map { prefs -> prefs[AudioArEnabledKey] ?: false }
                .distinctUntilChanged()
                .collect { enabled ->
                    isEnabled = enabled
                    if (enabled) start() else stop()
                }
        }

        scope.launch {
            dataStore.data
                .map { prefs -> prefs[AudioArAutoCalibrateKey] ?: false }
                .distinctUntilChanged()
                .collect { autoCalibrate = it }
        }

        scope.launch {
            dataStore.data
                .map { prefs -> prefs[AudioArSensitivityKey] ?: 1f }
                .distinctUntilChanged()
                .collect { sensitivity = it }
        }
    }

    private fun start() {
        try {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }

            Timber.d("SpatialAudioManager: Sensors started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start spatial audio sensors")
        }
    }

    private fun stop() {
        try {
            sensorManager.unregisterListener(this)
            Timber.d("SpatialAudioManager: Sensors stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop spatial audio sensors")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isEnabled) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
            }
        }

        updateOrientation()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }

    private fun updateOrientation() {
        // Get rotation matrix from accelerometer and magnetometer
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // Get orientation angles (azimuth, pitch, roll)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Calculate current device orientation in degrees
        val currentAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val currentPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val currentRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        // Apply auto-calibration if head is stable
        if (autoCalibrate) {
            val isStable = isHeadStable(currentAzimuth, currentPitch, currentRoll)
            if (isStable && abs(currentAzimuth - centerAzimuth) < 5f) {
                updateCalibratedCenter(currentAzimuth, currentPitch, currentRoll)
            }
        }

        // Calculate soundstage rotation relative to calibration center
        var azimuthDelta = currentAzimuth - centerAzimuth
        // Normalize to -180 to 180
        while (azimuthDelta > 180f) azimuthDelta -= 360f
        while (azimuthDelta < -180f) azimuthDelta += 360f

        // Apply sensitivity multiplier (0.5x to 2.5x)
        currentSoundstageRotation = azimuthDelta * sensitivity

        // Calculate soundstage depth from pitch and roll
        val pitchDelta = currentPitch - centerPitch
        val rollDelta = currentRoll - centerRoll
        val totalTilt = sqrt(pitchDelta * pitchDelta + rollDelta * rollDelta) / 90f
        currentSoundstageDepth = (1f + (totalTilt * sensitivity * 0.5f)).coerceIn(0.5f, 2.5f)
    }

    private fun isHeadStable(azimuth: Float, pitch: Float, roll: Float): Boolean {
        // Check if motion is below threshold (head stable)
        return abs(pitch) < 10f && abs(roll) < 10f
    }

    private fun updateCalibratedCenter(azimuth: Float, pitch: Float, roll: Float) {
        // Slowly converge to stable position
        centerAzimuth = centerAzimuth * 0.9f + azimuth * 0.1f
        centerPitch = centerPitch * 0.9f + pitch * 0.1f
        centerRoll = centerRoll * 0.9f + roll * 0.1f
    }

    /**
     * Recenter the audio soundstage to current device orientation.
     * Useful when user wants to reset after head movement.
     */
    fun recenter() {
        centerAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        centerPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        centerRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        scope.launch {
            dataStore.edit { prefs ->
                prefs[AudioArCenterPointKey] = "$centerAzimuth,$centerPitch,$centerRoll"
            }
        }
        Timber.d("SpatialAudioManager: Recentered audio at Az:$centerAzimuth Pi:$centerPitch Ro:$centerRoll")
    }

    /**
     * Load calibration point from storage.
     */
    fun loadCalibrationPoint() {
        scope.launch {
            dataStore.data.collect { prefs ->
                val centerPoint = prefs[AudioArCenterPointKey]
                if (centerPoint != null) {
                    val parts = centerPoint.split(",")
                    if (parts.size == 3) {
                        try {
                            centerAzimuth = parts[0].toFloat()
                            centerPitch = parts[1].toFloat()
                            centerRoll = parts[2].toFloat()
                            Timber.d("SpatialAudioManager: Loaded calibration point")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to parse calibration point")
                        }
                    }
                }
            }
        }
    }

    fun release() {
        stop()
    }
}
