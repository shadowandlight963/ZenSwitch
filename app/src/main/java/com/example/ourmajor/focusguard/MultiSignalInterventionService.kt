package com.example.ourmajor.focusguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ourmajor.R

/**
 * Multi-signal intervention service for Focus Guard.
 * Provides multiple intervention signals when user exceeds time limits.
 */
class MultiSignalInterventionService : Service() {

    companion object {
        private const val TAG = "MultiSignalIntervention"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "intervention_service_channel"
        
        const val ACTION_START_INTERVENTION = "com.example.ourmajor.action.START_INTERVENTION"
        const val ACTION_STOP_INTERVENTION = "com.example.ourmajor.action.STOP_INTERVENTION"
        const val ACTION_STOP = "com.example.ourmajor.action.STOP"
        const val ACTION_EMERGENCY_OVERRIDE = "com.example.ourmajor.action.EMERGENCY_OVERRIDE"
    }

    private var isInterventionActive = false
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        Log.d(TAG, "MultiSignalInterventionService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_INTERVENTION -> startIntervention()
            ACTION_STOP_INTERVENTION -> stopIntervention()
            ACTION_STOP -> stopIntervention()
            ACTION_EMERGENCY_OVERRIDE -> {
                Log.d(TAG, "Emergency override - stopping intervention")
                stopIntervention()
            }
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startIntervention() {
        if (isInterventionActive) {
            Log.d(TAG, "Intervention already active")
            return
        }
        
        isInterventionActive = true
        Log.d(TAG, "Starting multi-signal intervention")
        
        // Start foreground service with notification
        val notification = createInterventionNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Here you can add multiple intervention signals:
        // - Visual notifications
        // - Sound alerts
        // - Vibration patterns
        // - Overlay dialogs
        // etc.
    }

    private fun stopIntervention() {
        if (!isInterventionActive) {
            Log.d(TAG, "Intervention not active")
            return
        }
        
        isInterventionActive = false
        Log.d(TAG, "Stopping multi-signal intervention")
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Intervention",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for focus time limit interventions"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createInterventionNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Time Limit Reached")
            .setContentText("You've reached your daily time limit. Take a mindful break.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Take Break",
                createStopIntent()
            )
            .build()
    }

    private fun createStopIntent(): PendingIntent {
        val intent = Intent(this, MultiSignalInterventionService::class.java).apply {
            action = ACTION_STOP_INTERVENTION
        }
        return android.app.PendingIntent.getService(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        isInterventionActive = false
        Log.d(TAG, "MultiSignalInterventionService destroyed")
    }
}
