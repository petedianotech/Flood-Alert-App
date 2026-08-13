package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class VibrationDetector(
    context: Context,
    private val onTriggerCallback: (peakDelta: Float) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _currentDelta = MutableStateFlow(0.0f)
    val currentDelta: StateFlow<Float> = _currentDelta.asStateFlow()

    private val _currentGyroMagnitude = MutableStateFlow(0.0f)
    val currentGyroMagnitude: StateFlow<Float> = _currentGyroMagnitude.asStateFlow()

    private val _compositeVibrationScore = MutableStateFlow(0.0f)
    val compositeVibrationScore: StateFlow<Float> = _compositeVibrationScore.asStateFlow()

    private val _continuousDurationMs = MutableStateFlow(0L)
    val continuousDurationMs: StateFlow<Long> = _continuousDurationMs.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    var thresholdDelta: Float = 1.5f
    var requiredDurationMs: Long = 3000L

    private var continuousStartTimestampMs: Long? = null
    private var maxDeltaDuringVibration: Float = 0.0f
    private var lastTriggerTimeMs: Long = 0L
    private var latestGyroRadSec: Float = 0.0f

    fun startListening() {
        if (_isMonitoring.value) return
        var registeredAny = false

        accelerometer?.let { accel ->
            try {
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
                registeredAny = true
                Log.d(TAG, "Accelerometer listener registered with SENSOR_DELAY_GAME")
            } catch (e: Exception) {
                Log.w(TAG, "Failed SENSOR_DELAY_GAME for accelerometer, falling back to SENSOR_DELAY_NORMAL", e)
                try {
                    sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
                    registeredAny = true
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed SENSOR_DELAY_NORMAL for accelerometer", e2)
                }
            }
        } ?: run {
            Log.e(TAG, "Accelerometer sensor not found on this device!")
        }

        gyroscope?.let { gyro ->
            try {
                sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
                registeredAny = true
                Log.d(TAG, "Gyroscope listener registered with SENSOR_DELAY_GAME")
            } catch (e: Exception) {
                Log.w(TAG, "Failed SENSOR_DELAY_GAME for gyroscope, falling back to SENSOR_DELAY_NORMAL", e)
                try {
                    sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
                    registeredAny = true
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed SENSOR_DELAY_NORMAL for gyroscope", e2)
                }
            }
        }

        if (registeredAny) {
            _isMonitoring.value = true
        }
    }

    fun stopListening() {
        if (!_isMonitoring.value) return
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering sensor listener", e)
        }
        _isMonitoring.value = false
        continuousStartTimestampMs = null
        _currentDelta.value = 0.0f
        _currentGyroMagnitude.value = 0.0f
        _compositeVibrationScore.value = 0.0f
        _continuousDurationMs.value = 0L
        Log.d(TAG, "Sensor listeners unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val gx = event.values[0]
            val gy = event.values[1]
            val gz = event.values[2]
            latestGyroRadSec = sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat()
            _currentGyroMagnitude.value = latestGyroRadSec
            return
        }

        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Formula: Vector Magnitude A = sqrt(X^2 + Y^2 + Z^2), Delta = |A - 9.81|
        val vectorMagnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = abs(vectorMagnitude - 9.81f)

        _currentDelta.value = delta

        val currentTimeMs = System.currentTimeMillis()

        if (delta > thresholdDelta) {
            if (delta > maxDeltaDuringVibration) {
                maxDeltaDuringVibration = delta
            }

            val startTime = continuousStartTimestampMs ?: currentTimeMs.also {
                continuousStartTimestampMs = it
                maxDeltaDuringVibration = delta
            }

            val elapsedMs = currentTimeMs - startTime
            _continuousDurationMs.value = elapsedMs

            // Check if continuous vibration exceeds required duration (e.g., 3000ms)
            if (elapsedMs >= requiredDurationMs) {
                // Cooldown check (prevent repeated triggers within 10 seconds)
                if (currentTimeMs - lastTriggerTimeMs > 10_000L) {
                    lastTriggerTimeMs = currentTimeMs
                    val peak = maxDeltaDuringVibration
                    Log.w(TAG, "FLOOD VIBRATION TRIGGERED! Continuous $elapsedMs ms at peak delta $peak")
                    onTriggerCallback(peak)
                }
                // Reset tracker after triggering
                continuousStartTimestampMs = null
                maxDeltaDuringVibration = 0.0f
                _continuousDurationMs.value = 0L
            }
        } else {
            // Single shock or quick tap ended - reset continuous tracking
            if (continuousStartTimestampMs != null) {
                continuousStartTimestampMs = null
                maxDeltaDuringVibration = 0.0f
                _continuousDurationMs.value = 0L
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val TAG = "VibrationDetector"
    }
}
