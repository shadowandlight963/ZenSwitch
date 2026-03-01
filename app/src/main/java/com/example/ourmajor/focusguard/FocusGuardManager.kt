package com.example.ourmajor.focusguard

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Public API for Focus Guard system.
 * 
 * Provides simple interface for UI components to interact with Focus Guard.
 */
object FocusGuardManager {
    
    private const val TAG = "FocusGuardManager"
    
    /**
     * Initialize Focus Guard system
     */
    fun initialize(context: Context) {
        Log.i(TAG, "Initializing Focus Guard system")
        
        // Initialize state persistence
        val dailyLimit = StateBasedPersistence.getDailyLimit(context)
        Log.d(TAG, "Daily limit: ${dailyLimit / 60_000} minutes")
    }
    
    /**
     * Start Focus Guard monitoring
     */
    fun startMonitoring(context: Context) {
        Log.i(TAG, "Starting Focus Guard monitoring")
        
        val intent = Intent(context, FocusGuardCoreService::class.java).apply {
            action = FocusGuardCoreService.ACTION_START_MONITORING
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    /**
     * Stop Focus Guard monitoring
     */
    fun stopMonitoring(context: Context) {
        Log.i(TAG, "Stopping Focus Guard monitoring")
        
        val intent = Intent(context, FocusGuardCoreService::class.java).apply {
            action = FocusGuardCoreService.ACTION_STOP_MONITORING
        }
        
        context.startService(intent)
    }
    
    /**
     * Set daily time limit
     */
    fun setDailyLimit(context: Context, limitMinutes: Int) {
        val limitMs = limitMinutes * 60_000L
        StateBasedPersistence.setDailyLimit(context, limitMs)
        Log.i(TAG, "Daily limit set to $limitMinutes minutes")
    }
    
    /**
     * Get daily time limit
     */
    fun getDailyLimit(context: Context): Int {
        val limitMs = StateBasedPersistence.getDailyLimit(context)
        return (limitMs / 60_000).toInt()
    }
    
    /**
     * Get current usage statistics
     */
    suspend fun getCurrentUsage(context: Context): UsageStats {
        val tracker = DeterministicUsageTracker(context)
        val usage = tracker.getCurrentUsage()
        val totalMs = usage.values.sum()
        val limitMs = StateBasedPersistence.getDailyLimit(context)
        
        return UsageStats(
            totalUsageMs = totalMs,
            dailyLimitMs = limitMs,
            packageBreakdown = usage,
            isOverLimit = totalMs >= limitMs,
            percentageUsed = (totalMs.toDouble() / limitMs * 100).toInt()
        )
    }
    
    /**
     * Check if intervention is currently active
     */
    fun isInterventionActive(context: Context): Boolean {
        return StateBasedPersistence.isInterventionActive(context)
    }
    
    /**
     * Check if emergency override is available
     */
    fun canUseEmergencyOverride(context: Context): Boolean {
        return StateBasedPersistence.canUseEmergencyOverride(context) &&
               !StateBasedPersistence.isEmergencyOverrideActive(context)
    }
    
    /**
     * Get system state for debugging
     */
    fun getSystemState(context: Context): String {
        return StateBasedPersistence.dumpState(context)
    }
    
    /**
     * Force emergency override (for testing only)
     */
    fun forceEmergencyOverride(context: Context) {
        if (canUseEmergencyOverride(context)) {
            val intent = Intent(context, MultiSignalInterventionService::class.java).apply {
                action = MultiSignalInterventionService.ACTION_EMERGENCY_OVERRIDE
            }
            context.startService(intent)
        }
    }
}

/**
 * Usage statistics data class
 */
data class UsageStats(
    val totalUsageMs: Long,
    val dailyLimitMs: Long,
    val packageBreakdown: Map<String, Long>,
    val isOverLimit: Boolean,
    val percentageUsed: Int
) {
    val totalUsageMinutes: Int get() = (totalUsageMs / 60_000).toInt()
    val dailyLimitMinutes: Int get() = (dailyLimitMs / 60_000).toInt()
    val remainingMinutes: Int get() = maxOf(0, dailyLimitMinutes - totalUsageMinutes)
}
