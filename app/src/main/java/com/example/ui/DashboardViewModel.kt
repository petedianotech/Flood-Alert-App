package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlarmActivity
import com.example.data.AppNodeMode
import com.example.data.FloodAlert
import com.example.data.FloodRepository
import com.example.data.UserPreferencesManager
import com.example.sensor.VibrationDetector
import com.example.service.FloodDetectorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(context: Context) : ViewModel() {

    private val preferencesManager = UserPreferencesManager(context.applicationContext)
    private val repository = FloodRepository(context.applicationContext)

    val userName: StateFlow<String> = preferencesManager.userName
    val villageName: StateFlow<String> = preferencesManager.villageName
    val isLoggedIn: StateFlow<Boolean> = preferencesManager.isLoggedIn

    val nodeMode: StateFlow<AppNodeMode> = preferencesManager.nodeMode
    val vibrationThreshold: StateFlow<Float> = preferencesManager.vibrationThreshold
    val durationRequirementMs: StateFlow<Long> = preferencesManager.durationRequirementMs
    val deviceName: StateFlow<String> = preferencesManager.deviceName
    val locationName: StateFlow<String> = preferencesManager.locationName

    fun loginUser(name: String, village: String, password: String = "") {
        preferencesManager.saveUserProfile(name, village, password)
    }

    fun logoutUser() {
        preferencesManager.logout()
    }

    val recentAlerts: StateFlow<List<FloodAlert>> = repository.getRecentAlertsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isServiceRunning = MutableStateFlow(FloodDetectorService.isRunning)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    // Local vibration detector for real-time live preview when in app dashboard
    private val liveDetector = VibrationDetector(context.applicationContext) { peakDelta ->
        // Triggered via local preview sensor if active
        if (nodeMode.value == AppNodeMode.SENSOR_UNIT) {
            publishAndTriggerAlert(context, peakDelta)
        }
    }

    val liveDelta: StateFlow<Float> = liveDetector.currentDelta
    val liveDurationMs: StateFlow<Long> = liveDetector.continuousDurationMs

    init {
        // Automatically start live preview vibration meter on dashboard
        liveDetector.thresholdDelta = vibrationThreshold.value
        liveDetector.requiredDurationMs = durationRequirementMs.value
        liveDetector.startListening()
    }

    fun setNodeMode(mode: AppNodeMode, context: Context) {
        preferencesManager.setNodeMode(mode)
        if (mode == AppNodeMode.CITIZEN_NODE && FloodDetectorService.isRunning) {
            FloodDetectorService.stopService(context)
            _isServiceRunning.value = false
        }
    }

    fun updateThreshold(threshold: Float) {
        preferencesManager.setVibrationThreshold(threshold)
        liveDetector.thresholdDelta = threshold
    }

    fun updateDurationMs(durationMs: Long) {
        preferencesManager.setDurationRequirementMs(durationMs)
        liveDetector.requiredDurationMs = durationMs
    }

    fun updateDeviceDetails(name: String, location: String) {
        preferencesManager.setDeviceName(name)
        preferencesManager.setLocationName(location)
    }

    fun toggleMonitoring(context: Context) {
        if (FloodDetectorService.isRunning) {
            FloodDetectorService.stopService(context)
            _isServiceRunning.value = false
        } else {
            FloodDetectorService.startService(context)
            _isServiceRunning.value = true
        }
    }

    fun refreshServiceStatus() {
        _isServiceRunning.value = FloodDetectorService.isRunning
    }

    fun triggerTestAlarm(context: Context) {
        publishAndTriggerAlert(context, peakDelta = 3.2f)
    }

    private fun publishAndTriggerAlert(context: Context, peakDelta: Float) {
        viewModelScope.launch {
            val alert = FloodAlert(
                timestamp = System.currentTimeMillis(),
                deviceId = Build.MODEL ?: "DeviceSensor",
                deviceName = deviceName.value,
                locationName = locationName.value,
                peakDelta = peakDelta,
                severity = "CRITICAL",
                triggerSource = "MANUAL_TEST_OR_SENSOR"
            )

            repository.publishAlert(alert)

            val intent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(AlarmActivity.EXTRA_LOCATION, locationName.value)
                putExtra(AlarmActivity.EXTRA_DEVICE_NAME, deviceName.value)
                putExtra(AlarmActivity.EXTRA_PEAK_DELTA, peakDelta)
            }
            context.startActivity(intent)
        }
    }

    override fun onCleared() {
        liveDetector.stopListening()
        super.onCleared()
    }
}
