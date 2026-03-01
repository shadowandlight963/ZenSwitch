package com.example.ourmajor.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ourmajor.R
import com.example.ourmajor.ui.activities.TestActivity

/**
 * Production-safe notification helper for ZenSwitch nudges.
 * Handles Android 8+ channels and Android 13+ permissions.
 */
class NudgeNotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NUDGE_DEBUG"
        private const val CHANNEL_ID = "focus_nudge_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val notificationManager: NotificationManager? by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    /**
     * Creates notification channel once at app start (Android 8+)
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Focus Nudges",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for mindful nudges"
                    enableLights(true)
                    enableVibration(true)
                }
                
                notificationManager?.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $CHANNEL_ID")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channel", e)
            }
        }
    }

    /**
     * Shows test nudge notification with proper permission handling
     */
    fun showTestNudge() {
        Log.d(TAG, "Test nudge triggered")
        
        // Check notification manager availability
        if (notificationManager == null) {
            Log.e(TAG, "Notification manager is null")
            showToast("Notification service unavailable")
            return
        }

        // Check Android 13+ permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                showToast("Notification permission required. Please enable in Settings.")
                return
            }
        }

        // Ensure channel exists
        createNotificationChannel()

        try {
            val notification = buildNotification()
            notificationManager?.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Test nudge notification sent successfully")
            showToast("Test nudge sent!")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when sending notification", e)
            showToast("Permission denied for notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
            showToast("Failed to send notification")
        }
    }

    /**
     * Builds the high-priority notification
     */
    private fun buildNotification() = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Mindful Break")
        .setContentText("This is a test nudge. Tap to reset.")
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setContentIntent(createPendingIntent())
        .build()

    /**
     * Creates PendingIntent to open TestActivity
     */
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, TestActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    /**
     * Checks Android 13+ notification permission
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required before Android 13
        }
    }

    /**
     * Shows toast message safely
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
