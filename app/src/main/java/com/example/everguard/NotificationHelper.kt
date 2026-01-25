package com.example.everguard


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    // Unique IDs for the channel and the notifications
    private val CHANNEL_ID = "accident_alerts"
    private val CHANNEL_NAME = "Accident Alerts"

    init {
        createNotificationChannel()
    }

    // In NotificationHelper.kt
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_HIGH is what enables the "hover" (Heads-up) effect
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Emergency alerts for Everguard"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                // This ensures it makes sound even in background
                setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun sendLocalNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning) // Ensure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            // PRIORITY_HIGH is required for the "hover" effect on older Android versions
            .setPriority(NotificationCompat.PRIORITY_MAX) // Use MAX instead of HIGH
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(1000, 1000, 1000)) // Force vibration
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Categorize as an alarm
            .setOngoing(false)
            .setAutoCancel(true)

        val intent =
            Intent(context, HomeNotificationsContactsActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}