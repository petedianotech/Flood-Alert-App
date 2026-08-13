package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("flood_alert_prefs", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(getSavedUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _villageName = MutableStateFlow(getSavedVillageName())
    val villageName: StateFlow<String> = _villageName.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(getSavedIsLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _nodeMode = MutableStateFlow(getSavedMode())
    val nodeMode: StateFlow<AppNodeMode> = _nodeMode.asStateFlow()

    private val _vibrationThreshold = MutableStateFlow(getSavedThreshold())
    val vibrationThreshold: StateFlow<Float> = _vibrationThreshold.asStateFlow()

    private val _durationRequirementMs = MutableStateFlow(getSavedDurationMs())
    val durationRequirementMs: StateFlow<Long> = _durationRequirementMs.asStateFlow()

    private val _deviceName = MutableStateFlow(getSavedDeviceName())
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _locationName = MutableStateFlow(getSavedLocationName())
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    fun saveUserProfile(name: String, village: String, password: String = "") {
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_VILLAGE_NAME, village)
            .putString(KEY_LOCATION_NAME, if (village.isNotBlank()) "$village Station" else getSavedLocationName())
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        if (password.isNotEmpty()) {
            prefs.edit().putString(KEY_USER_PASSWORD, password).apply()
        }
        _userName.value = name
        _villageName.value = village
        _locationName.value = if (village.isNotBlank()) "$village Station" else getSavedLocationName()
        _isLoggedIn.value = true
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
        _isLoggedIn.value = false
    }

    fun setNodeMode(mode: AppNodeMode) {
        prefs.edit().putString(KEY_NODE_MODE, mode.name).apply()
        _nodeMode.value = mode
    }

    fun setVibrationThreshold(threshold: Float) {
        prefs.edit().putFloat(KEY_THRESHOLD, threshold).apply()
        _vibrationThreshold.value = threshold
    }

    fun setDurationRequirementMs(durationMs: Long) {
        prefs.edit().putLong(KEY_DURATION_MS, durationMs).apply()
        _durationRequirementMs.value = durationMs
    }

    fun setDeviceName(name: String) {
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
        _deviceName.value = name
    }

    fun setLocationName(location: String) {
        prefs.edit().putString(KEY_LOCATION_NAME, location).apply()
        _locationName.value = location
    }

    private fun getSavedUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    private fun getSavedVillageName(): String = prefs.getString(KEY_VILLAGE_NAME, "") ?: ""
    private fun getSavedIsLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    private fun getSavedMode(): AppNodeMode {
        val name = prefs.getString(KEY_NODE_MODE, AppNodeMode.SENSOR_UNIT.name)
        return try {
            AppNodeMode.valueOf(name ?: AppNodeMode.SENSOR_UNIT.name)
        } catch (e: Exception) {
            AppNodeMode.SENSOR_UNIT
        }
    }

    private fun getSavedThreshold(): Float = prefs.getFloat(KEY_THRESHOLD, 1.5f)
    private fun getSavedDurationMs(): Long = prefs.getLong(KEY_DURATION_MS, 3000L)
    private fun getSavedDeviceName(): String = prefs.getString(KEY_DEVICE_NAME, "Sensor Unit Alpha") ?: "Sensor Unit Alpha"
    private fun getSavedLocationName(): String = prefs.getString(KEY_LOCATION_NAME, "Main Water Line") ?: "Main Water Line"

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_VILLAGE_NAME = "village_name"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_NODE_MODE = "node_mode"
        private const val KEY_THRESHOLD = "vibration_threshold"
        private const val KEY_DURATION_MS = "duration_ms"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_LOCATION_NAME = "location_name"
    }
}

