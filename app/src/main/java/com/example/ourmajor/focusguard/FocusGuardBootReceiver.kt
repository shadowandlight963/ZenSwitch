package com.example.ourmajor.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Boot receiver for OEM survival and service persistence.
 * 
 * Strategy: Use WorkManager to restart service after device reboots
 * and handle OEM kills through periodic checks.
 */
class FocusGuardBootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "FocusGuardBoot"
        private const val BOOT_WORK_TAG = "focus_guard_boot"
        private const val PERIODIC_CHECK_WORK_TAG = "focus_guard_periodic"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.i(TAG, "Boot/restart detected, scheduling service restart")
                scheduleServiceRestart(context)
            }
        }
    }
    
    private fun scheduleServiceRestart(context: Context) {
        // Use WorkManager for reliable, battery-aware scheduling
        val bootWorkRequest = OneTimeWorkRequestBuilder<ServiceRestartWorker>()
            .addTag(BOOT_WORK_TAG)
            .setInitialDelay(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 5L else 10L, TimeUnit.SECONDS) // Delay for system stability
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "focus_guard_boot_restart",
            ExistingWorkPolicy.REPLACE,
            bootWorkRequest
        )
        
        // Also schedule periodic checks for OEM survival
        schedulePeriodicChecks(context)
    }
    
    private fun schedulePeriodicChecks(context: Context) {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<ServiceCheckWorker>(
            15, TimeUnit.MINUTES // Minimum interval for reliability
        )
            .addTag(PERIODIC_CHECK_WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "focus_guard_periodic_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
        
        Log.i(TAG, "Periodic service checks scheduled")
    }
}

/**
 * Worker that handles service restart after boot.
 */
class ServiceRestartWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ServiceRestartWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Restarting Focus Guard service after boot")
            
            // Check if Focus Guard was enabled before reboot
            val wasEnabled = StateBasedPersistence.isInterventionActive(applicationContext)
            
            if (wasEnabled) {
                // Start the core service
                val intent = Intent(applicationContext, FocusGuardCoreService::class.java).apply {
                    action = FocusGuardCoreService.ACTION_START_MONITORING
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    applicationContext.startService(intent)
                }
                
                Log.i(TAG, "Focus Guard service restarted successfully")
                Result.success()
            } else {
                Log.i(TAG, "Focus Guard was not enabled, skipping restart")
                Result.success()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart Focus Guard service", e)
            Result.retry()
        }
    }
}

/**
 * Worker that periodically checks if service is still running.
 */
class ServiceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ServiceCheckWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Checking Focus Guard service status")
            
            // Check if service should be running
            val shouldBeRunning = StateBasedPersistence.isInterventionActive(applicationContext) ||
                                StateBasedPersistence.hasLimitCrossedToday(applicationContext)
            
            if (shouldBeRunning) {
                // Try to check if service is actually running by sending a check intent
                val checkIntent = Intent(applicationContext, FocusGuardCoreService::class.java).apply {
                    action = FocusGuardCoreService.ACTION_CHECK_STATE
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(checkIntent)
                } else {
                    applicationContext.startService(checkIntent)
                }
                
                Log.d(TAG, "Service check sent")
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Service check failed", e)
            Result.retry()
        }
    }
}
