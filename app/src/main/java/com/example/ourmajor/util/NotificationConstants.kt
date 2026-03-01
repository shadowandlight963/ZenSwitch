package com.example.ourmajor.util

/**
 * Centralized notification IDs and request codes to prevent collisions.
 * 
 * Notification ID ranges:
 * - 1000-1099: Focus Monitor Service notifications
 * - 2000-2099: Reminder notifications
 * - 3000-3099: PendingIntents request codes
 */
object NotificationConstants {
    
    // Focus Monitor Service Notification IDs
    const val NOTIFICATION_ID_FOCUS_FOREGROUND = 1001
    const val NOTIFICATION_ID_FOCUS_NUDGE = 1002
    
    // Reminder Notification IDs
    const val NOTIFICATION_ID_REMINDER = 2001
    
    // PendingIntent Request Codes
    const val REQUEST_CODE_REMINDER_PENDING_INTENT = 3001
    const val REQUEST_CODE_FOCUS_NUDGE_PENDING_INTENT = 3002
    
    // Channel IDs
    const val CHANNEL_ID_FOCUS_MONITOR = "zenswitch_focus_monitor"
    const val CHANNEL_ID_FOCUS_NUDGE = "zenswitch_nudge"
    const val CHANNEL_ID_DAILY_REMINDER = "daily_reminder"
}
