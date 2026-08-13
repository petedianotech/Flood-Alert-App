package com.example.data

import com.google.firebase.Timestamp

data class FloodAlert(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val deviceName: String = "Sensor Unit #1",
    val locationName: String = "Water Station Alpha",
    val peakDelta: Float = 0.0f,
    val severity: String = "CRITICAL",
    val triggerSource: String = "ACCELEROMETER_VIBRATION",
    val formattedTime: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "timestamp" to Timestamp.now(),
            "timestampMs" to timestamp,
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "locationName" to locationName,
            "peakDelta" to peakDelta,
            "severity" to severity,
            "triggerSource" to triggerSource
        )
    }

    companion object {
        fun fromFirestoreMap(id: String, data: Map<String, Any?>): FloodAlert {
            val ts = (data["timestamp"] as? Timestamp)?.toDate()?.time
                ?: (data["timestampMs"] as? Long)
                ?: System.currentTimeMillis()
            
            return FloodAlert(
                id = id,
                timestamp = ts,
                deviceId = data["deviceId"] as? String ?: "Unknown",
                deviceName = data["deviceName"] as? String ?: "Sensor Unit",
                locationName = data["locationName"] as? String ?: "Main Station",
                peakDelta = (data["peakDelta"] as? Number)?.toFloat() ?: 0.0f,
                severity = data["severity"] as? String ?: "CRITICAL",
                triggerSource = data["triggerSource"] as? String ?: "AUTOMATIC"
            )
        }
    }
}

enum class AppNodeMode {
    SENSOR_UNIT,  // Attached to pipe / vibrator / water structure
    CITIZEN_NODE  // Receiver phone for family members / community
}
