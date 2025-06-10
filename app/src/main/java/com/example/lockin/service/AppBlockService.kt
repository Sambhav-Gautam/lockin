package com.example.lockin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AppBlockService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(15, createNotificationChannel())
    }

    private fun createNotificationChannel(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "lockin_service_channel",
                "LockIn Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, "lockin_service_channel")
            .setContentTitle("LockIn Running")
            .setContentText("Monitoring and blocking selected apps")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        return null
    }
}