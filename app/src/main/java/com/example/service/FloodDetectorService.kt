package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AlarmActivity
import com.example.MainActivity
import com.example.R
import com.example.data.FloodAlert
import com.example.data.FloodRepository
import com.example.data.UserPreferencesManager
import com.example.sensor.VibrationDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FloodDetectorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var vibrationDetector: VibrationDetector
    private lateinit var preferencesManager: UserPreferencesManager
    private lateinit var repository: FloodRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloodDetectorService created")

        preferencesManager = UserPreferencesManager(applicationContext)
        repository = FloodRepository(applicationContext)

        vibrationDetector = VibrationDetector(applicationContext) { peakDelta ->
            onFloodVibrationTriggered(peakDelta)
        }

        // Keep detector parameters updated from preferences
        preferencesManager.vibrationThreshold.onEach { threshold ->
            vibrationDetector.thresholdDelta = threshold
        }.launchIn(serviceScope)

        preferencesManager.durationRequirementMs.onEach { durationMs ->
            vibrationDetector.requiredDurationMs = durationMs
        }.launchIn(serviceScope)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceWithNotification()
        vibrationDetector.startListening()
        isRunning = true

        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildForegroundNotification("Monitoring Active")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground with specialUse, falling back", e)
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to startForeground fallback", e2)
            }
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloodDetectorService::class.java).apply {
            this.action = ACTION_STOP_SERVICE
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flood Alert Sensor Node")
            .setContentText("Status: $statusText • Accelerometer Active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingOpenIntent)
            .addAction(0, "Disarm", pendingStopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flood Sensor Foreground Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows persistent status when flood sensor monitoring is active"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun onFloodVibrationTriggered(peakDelta: Float) {
        val deviceName = preferencesManager.deviceName.value
        val locationName = preferencesManager.locationName.value

        // 1. Publish event to Firebase Firestore
        val alert = FloodAlert(
            timestamp = System.currentTimeMillis(),
            deviceId = Build.MODEL ?: "DeviceSensor",
            deviceName = deviceName,
            locationName = locationName,
            peakDelta = peakDelta,
            severity = "CRITICAL",
            triggerSource = "ACCELEROMETER_VIBRATION"
        )
        repository.publishAlert(alert)

        // 2. Launch AlarmActivity immediately at full volume over lock screen
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmActivity.EXTRA_LOCATION, locationName)
            putExtra(AlarmActivity.EXTRA_DEVICE_NAME, deviceName)
            putExtra(AlarmActivity.EXTRA_PEAK_DELTA, peakDelta)
        }
        startActivity(alarmIntent)
    }

    private fun stopMonitoring() {
        vibrationDetector.stopListening()
        isRunning = false
    }

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "FloodDetectorService"
        const val CHANNEL_ID = "flood_sensor_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.service.STOP_SERVICE"

        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            try {
                val intent = Intent(context, FloodDetectorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting FloodDetectorService", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, FloodDetectorService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service via startService", e)
                try {
                    context.stopService(Intent(context, FloodDetectorService::class.java))
                } catch (e2: Exception) {
                    Log.e(TAG, "Error stopping service via stopService", e2)
                }
            }
            isRunning = false
        }
    }
}
