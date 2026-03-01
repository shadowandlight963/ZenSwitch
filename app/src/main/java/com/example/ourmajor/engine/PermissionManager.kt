package com.example.ourmajor.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.ourmajor.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Gatekeeper for ZenSwitch Focus Engine: Usage Access, Notifications, Battery Optimization.
 * Call [checkAndRequestPermissions] after login so the user grants permissions before using Focus Guard.
 */
object PermissionManager {

    /**
     * Returns true if the app has been granted "Usage access" (PACKAGE_USAGE_STATS) by the user.
     * Required for usage tracking.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /** True if app can show notifications (Android 13+). */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /** True if app is exempt from battery optimization (recommended for Focus Guard). */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** True if app can draw over other apps (optional). */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Check critical permissions; if any missing, show a dialog and open the relevant Settings.
     * Call from Activity (e.g. MainActivity onResume). Returns true only when at least Usage Stats is granted.
     * Do not proceed to Dashboard until this returns true.
     */
    fun checkAndRequestPermissions(activity: Activity): Boolean {
        if (!hasUsageStatsPermission(activity)) {
            MaterialAlertDialogBuilder(activity, R.style.Theme_OurMajor)
                .setTitle(activity.getString(R.string.permission_usage_title))
                .setMessage(activity.getString(R.string.permission_usage_message))
                .setPositiveButton(activity.getString(R.string.permission_open_settings)) { _, _ ->
                    activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setCancelable(false)
                .show()
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(activity)) {
            MaterialAlertDialogBuilder(activity, R.style.Theme_OurMajor)
                .setTitle(activity.getString(R.string.permission_notification_title))
                .setMessage(activity.getString(R.string.permission_notification_message))
                .setPositiveButton(activity.getString(R.string.permission_open_settings)) { _, _ ->
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    }
                    activity.startActivity(intent)
                }
                .setCancelable(false)
                .show()
            return false
        }
        if (!isIgnoringBatteryOptimizations(activity)) {
            MaterialAlertDialogBuilder(activity, R.style.Theme_OurMajor)
                .setTitle(activity.getString(R.string.permission_battery_title))
                .setMessage(activity.getString(R.string.permission_battery_message))
                .setPositiveButton(activity.getString(R.string.permission_open_settings)) { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                }
                .setCancelable(false)
                .show()
            return false
        }
        if (!canDrawOverlays(activity)) {
            MaterialAlertDialogBuilder(activity, R.style.Theme_OurMajor)
                .setTitle(activity.getString(R.string.permission_overlay_title))
                .setMessage(activity.getString(R.string.permission_overlay_message))
                .setPositiveButton(activity.getString(R.string.permission_open_settings)) { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(intent)
                }
                .setCancelable(false)
                .show()
            return false
        }
        return true
    }
}
