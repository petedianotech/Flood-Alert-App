package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AlarmActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FloodMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("flood_alerts")
        } catch (e: Throwable) {
            Log.w(TAG, "Error subscribing to FCM topic on new token: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.w(TAG, "FCM Flood Alert Payload Received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val location = data["locationName"] ?: remoteMessage.notification?.title ?: "Water Station Alpha"
        val deviceName = data["deviceName"] ?: "Sensor Unit"
        val peakDeltaStr = data["peakDelta"] ?: "2.8"
        val peakDelta = peakDeltaStr.toFloatOrNull() ?: 2.8f

        // 1. Emergency Override Audio Volume to 100%
        setAlarmVolumeToMaximum()

        // 2. Launch Full-Screen Emergency Alarm Activity
        launchFullscreenAlarm(location, deviceName, peakDelta)
    }

    private fun setAlarmVolumeToMaximum() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxAlarmVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val maxMusicVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusicVol, 0)
            Log.d(TAG, "Successfully overrode alarm volume to 100% ($maxAlarmVol)")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting maximum volume", e)
        }
    }

    private fun launchFullscreenAlarm(location: String, deviceName: String, peakDelta: Float) {
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmActivity.EXTRA_LOCATION, location)
            putExtra(AlarmActivity.EXTRA_DEVICE_NAME, deviceName)
            putExtra(AlarmActivity.EXTRA_PEAK_DELTA, peakDelta)
        }

        // Fullscreen pending intent for lockscreen display
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_REQ_CODE,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        createHighPriorityChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚨 CRITICAL FLOOD DETECTED")
            .setContentText("Emergency vibration alert at $location ($deviceName)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EMERGENCY_NOTIF_ID, notification)

        // Directly launch full screen activity as well
        try {
            startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching full screen AlarmActivity directly", e)
        }
    }

    private fun createHighPriorityChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Critical Flood Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority full screen flood emergency alarms"
                setBypassDnd(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                setSound(null, audioAttributes)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "FloodMessagingService"
        const val CHANNEL_ID_EMERGENCY = "critical_flood_channel"
        const val EMERGENCY_NOTIF_ID = 9001
        const val NOTIFICATION_REQ_CODE = 2001
    }
}
