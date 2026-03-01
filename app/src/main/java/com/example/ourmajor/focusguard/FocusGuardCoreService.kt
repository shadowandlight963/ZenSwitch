package com.example.ourmajor.focusguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.currentCoroutineContext
import com.example.ourmajor.focusguard.DeterministicUsageTracker
import com.example.ourmajor.focusguard.StateBasedPersistence

/**
 * Core Focus Guard service that orchestrates detection and intervention.
 * 
 * Strategy: Persistent monitoring with state-based intervention restoration.
 */
class FocusGuardCoreService : Service() {
    
    companion object {
        private const val TAG = "FocusGuardCore"
        private const val NOTIFICATION_ID = 9000
        private const val CHANNEL_ID = "focus_guard_core"
        private const val CHANNEL_NAME = "Focus Guard Monitoring"
        
        const val ACTION_START_MONITORING = "start_monitoring"
        const val ACTION_STOP_MONITORING = "stop_monitoring"
        const val ACTION_CHECK_STATE = "check_state"
        
        // Monitoring intervals (adaptive based on usage)
        private const val INTERVAL_HIGH_USAGE = 10_000L      // 10 seconds
        private const val INTERVAL_MEDIUM_USAGE = 30_000L    // 30 seconds
        private const val INTERVAL_LOW_USAGE = 60_000L       // 1 minute
        private const val INTERVAL_EMERGENCY = 5_000L        // 5 seconds (during intervention)
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null
    private var usageTracker: DeterministicUsageTracker? = null
    private var notificationManager: NotificationManager? = null
    private var powerManager: PowerManager? = null
    
    override fun onCreate() {
        super.onCreate()
        usageTracker = DeterministicUsageTracker(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START_MONITORING -> {
                startMonitoring()
                START_STICKY
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                START_NOT_STICKY
            }
            ACTION_CHECK_STATE -> {
                checkAndRestoreState()
                START_STICKY
            }
            else -> START_NOT_STICKY
        }
    }
    
    private fun startMonitoring() {
        Log.i(TAG, "Starting Focus Guard monitoring")
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createMonitoringNotification())
        
        // Check and restore any existing intervention state
        checkAndRestoreState()
        
        // Start monitoring loop
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            runMonitoringLoop()
        }
    }
    
    private suspend fun runMonitoringLoop() {
        while (currentCoroutineContext()[Job]?.isActive == true) {
            try {
                // Check if emergency override is active
                if (StateBasedPersistence.isEmergencyOverrideActive(this@FocusGuardCoreService)) {
                    Log.d(TAG, "Emergency override active, skipping monitoring")
                    delay(INTERVAL_LOW_USAGE)
                    continue
                }
                
                // Get current usage
                val dailyLimit = StateBasedPersistence.getDailyLimit(this@FocusGuardCoreService)
                val result = usageTracker?.detectLimitCrossing(dailyLimit)
                
                if (result != null && result.error == null) {
                    StateBasedPersistence.setLastDetectionTime(this@FocusGuardCoreService, System.currentTimeMillis())
                    
                    if (result.isCrossed && !StateBasedPersistence.hasLimitCrossedToday(this@FocusGuardCoreService)) {
                        // Limit just crossed
                        handleLimitCrossed(result)
                    } else if (result.isCrossed && !StateBasedPersistence.isInterventionActive(this@FocusGuardCoreService)) {
                        // Intervention was stopped but limit still crossed - restart
                        handleLimitCrossed(result)
                    }
                    
                    updateMonitoringNotification(result)
                }
                
                // Adaptive monitoring interval
                val interval = if (StateBasedPersistence.isInterventionActive(this@FocusGuardCoreService)) {
                    INTERVAL_EMERGENCY
                } else if (result != null) {
                    when {
                        result.adjustedUsageMs >= dailyLimit * 0.9 -> INTERVAL_HIGH_USAGE
                        result.adjustedUsageMs >= dailyLimit * 0.7 -> INTERVAL_MEDIUM_USAGE
                        else -> INTERVAL_LOW_USAGE
                    }
                } else {
                    INTERVAL_LOW_USAGE
                }
                
                delay(interval)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in monitoring loop", e)
                delay(INTERVAL_LOW_USAGE)
            }
        }
    }
    
    private fun handleLimitCrossed(result: LimitCrossingResult) {
        Log.i(TAG, "Limit crossed: ${result.totalUsageMs / 60_000} minutes used")
        
        // Record the crossing
        StateBasedPersistence.setLimitCrossed(this, result.estimatedCrossingTime)
        StateBasedPersistence.incrementInterventionCount(this)
        
        // Start intervention service
        val interventionIntent = Intent(this, MultiSignalInterventionService::class.java).apply {
            action = MultiSignalInterventionService.ACTION_START_INTERVENTION
        }
        startService(interventionIntent)
    }
    
    private fun checkAndRestoreState() {
        Log.d(TAG, "Checking and restoring state")
        
        // Check if intervention should be active
        if (StateBasedPersistence.isInterventionActive(this) && 
            !StateBasedPersistence.isEmergencyOverrideActive(this)) {
            
            Log.i(TAG, "Restoring intervention state")
            val interventionIntent = Intent(this, MultiSignalInterventionService::class.java).apply {
                action = MultiSignalInterventionService.ACTION_START_INTERVENTION
            }
            startService(interventionIntent)
        }
        
        // Reset state if it's a new day
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % (24 * 60 * 60 * 1000))
        val lastDetection = StateBasedPersistence.getLastDetectionTime(this)
        
        if (lastDetection < startOfDay) {
            Log.i(TAG, "New day detected, resetting state")
            StateBasedPersistence.resetDailyState(this)
        }
    }
    
    private fun createMonitoringNotification(): Notification {
        val stopIntent = Intent(this, FocusGuardCoreService::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Focus Guard Active")
            .setContentText("Monitoring Instagram and YouTube usage")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }
    
    private fun updateMonitoringNotification(result: LimitCrossingResult) {
        val usageMinutes = result.totalUsageMs / 60_000
        val limitMinutes = StateBasedPersistence.getDailyLimit(this) / 60_000
        val percentage = (result.totalUsageMs.toDouble() / StateBasedPersistence.getDailyLimit(this) * 100).toInt()
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Focus Guard Active")
            .setContentText("$usageMinutes/$limitMinutes minutes used ($percentage%)")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(limitMinutes.toInt(), usageMinutes.toInt(), false)
            .build()
        
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    private fun stopMonitoring() {
        Log.i(TAG, "Stopping Focus Guard monitoring")
        
        monitoringJob?.cancel()
        
        // Stop intervention service
        val interventionIntent = Intent(this, MultiSignalInterventionService::class.java).apply {
            action = MultiSignalInterventionService.ACTION_STOP
        }
        startService(interventionIntent)
        
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Focus Guard monitoring service"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?) = null
}
