package com.example.ourmajor.focusguard

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Deterministic usage tracker that handles UsageStatsManager delays.
 * 
 * Key insight: UsageStatsManager batches events with 10-30s delays.
 * We cannot overcome this, but we can make detection deterministic.
 */
class DeterministicUsageTracker(private val context: Context) {
    
    companion object {
        private const val TAG = "DeterministicUsageTracker"
        private val TARGET_PACKAGES = listOf(
            "com.instagram.android",
            "com.google.android.youtube"
        )
        private val BATCH_COMPENSATION_MS = 30_000L // 30 seconds worst case
    }
    
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    
    /**
     * Detects if limit was crossed despite UsageStatsManager delays.
     * 
     * Strategy: Use sliding window with compensation for batch delays.
     */
    suspend fun detectLimitCrossing(dailyLimitMs: Long): LimitCrossingResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % TimeUnit.DAYS.toMillis(1))
        
        try {
            // Get usage events for today
            val usageEvents = usageStatsManager.queryEvents(startOfDay, now)
            val event = android.app.usage.UsageEvents.Event()
            
            val packageUsage = mutableMapOf<String, Long>()
            val lastForegroundTime = mutableMapOf<String, Long>()
            
            // Process all events
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                
                if (event.packageName in TARGET_PACKAGES) {
                    when (event.eventType) {
                        android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                            lastForegroundTime[event.packageName] = event.timeStamp
                        }
                        android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                        android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED -> {
                            val startTime = lastForegroundTime[event.packageName]
                            if (startTime != null) {
                                val duration = event.timeStamp - startTime
                                packageUsage[event.packageName] = (packageUsage[event.packageName] ?: 0L) + duration
                                lastForegroundTime.remove(event.packageName)
                            }
                        }
                    }
                }
            }
            
            // Add compensation for packages still in foreground (batch delay)
            for ((pkg, lastTime) in lastForegroundTime) {
                val liveDuration = now - lastTime
                packageUsage[pkg] = (packageUsage[pkg] ?: 0L) + liveDuration
            }
            
            val totalUsage = packageUsage.values.sum()
            val adjustedTotal = totalUsage + BATCH_COMPENSATION_MS // Compensate for worst-case delay
            
            val isCrossed = adjustedTotal >= dailyLimitMs
            val actualCrossingTime = if (isCrossed) {
                // Estimate when limit was actually crossed
                val usageRate = totalUsage / (now - startOfDay)
                val estimatedCrossingTime = startOfDay + (dailyLimitMs / usageRate)
                estimatedCrossingTime
            } else {
                -1L
            }
            
            LimitCrossingResult(
                isCrossed = isCrossed,
                totalUsageMs = totalUsage,
                adjustedUsageMs = adjustedTotal,
                estimatedCrossingTime = actualCrossingTime,
                packageBreakdown = packageUsage,
                detectionDelayMs = BATCH_COMPENSATION_MS
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect limit crossing", e)
            LimitCrossingResult.error(e)
        }
    }
    
    /**
     * Gets current usage with delay compensation
     */
    suspend fun getCurrentUsage(): Map<String, Long> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % TimeUnit.DAYS.toMillis(1))
        
        try {
            val usageEvents = usageStatsManager.queryEvents(startOfDay, now)
            val event = android.app.usage.UsageEvents.Event()
            
            val packageUsage = mutableMapOf<String, Long>()
            val lastForegroundTime = mutableMapOf<String, Long>()
            
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                
                if (event.packageName in TARGET_PACKAGES) {
                    when (event.eventType) {
                        android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                            lastForegroundTime[event.packageName] = event.timeStamp
                        }
                        android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                        android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED -> {
                            val startTime = lastForegroundTime[event.packageName]
                            if (startTime != null) {
                                val duration = event.timeStamp - startTime
                                packageUsage[event.packageName] = (packageUsage[event.packageName] ?: 0L) + duration
                                lastForegroundTime.remove(event.packageName)
                            }
                        }
                    }
                }
            }
            
            // Add live usage compensation
            for ((pkg, lastTime) in lastForegroundTime) {
                val liveDuration = now - lastTime
                packageUsage[pkg] = (packageUsage[pkg] ?: 0L) + liveDuration
            }
            
            packageUsage
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current usage", e)
            emptyMap()
        }
    }
}

data class LimitCrossingResult(
    val isCrossed: Boolean,
    val totalUsageMs: Long,
    val adjustedUsageMs: Long,
    val estimatedCrossingTime: Long,
    val packageBreakdown: Map<String, Long>,
    val detectionDelayMs: Long,
    val error: Exception? = null
) {
    companion object {
        fun error(exception: Exception) = LimitCrossingResult(
            isCrossed = false,
            totalUsageMs = 0L,
            adjustedUsageMs = 0L,
            estimatedCrossingTime = -1L,
            packageBreakdown = emptyMap(),
            detectionDelayMs = 0L,
            error = exception
        )
    }
}
