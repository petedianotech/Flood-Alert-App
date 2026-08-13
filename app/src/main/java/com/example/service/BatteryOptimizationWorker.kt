package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R

class BatteryOptimizationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        if (!isIgnoring) {
            sendWarningNotification()
        }

        return Result.success()
    }

    private fun sendWarningNotification() {
        val channelId = "battery_optimization_channel"
        val notificationId = 1002

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("LAUNCH_BATTERY_WHITELIST_DIALOG", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            201,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Battery Optimization Whitelist Warning",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Warns when the app needs to be exempted from battery optimizations for reliable flood monitoring."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔋 Battery Optimization Warning")
            .setContentText("Tap to whitelist Flood Alert for uninterrupted background flood monitoring.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
