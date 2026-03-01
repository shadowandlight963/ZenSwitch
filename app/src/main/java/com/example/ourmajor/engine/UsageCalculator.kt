package com.example.ourmajor.engine

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * Real-time usage engine: uses only queryEvents (no queryUsageStats).
 * Correctly counts the currently open session via lastOpenTime + (endTime - lastOpenTime[pkg]).
 */
object UsageCalculator {

    /**
     * Returns a map of package name -> total foreground duration in ms for today (midnight to endTime).
     * If a package is still in lastOpenTime after the loop, it is OPEN RIGHT NOW; we add (endTime - lastOpenTime[pkg]) to its total.
     */
    fun calculateUsage(context: Context, targetPackages: List<String>): Map<String, Long> {
        if (targetPackages.isEmpty()) return emptyMap()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val totalUsage = mutableMapOf<String, Long>()
        targetPackages.forEach { totalUsage[it] = 0L }
        val lastOpenTime = mutableMapOf<String, Long>()
        val targetSet = targetPackages.toSet()

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime) ?: return totalUsage
        val event = UsageEvents.Event()

        while (usageEvents.getNextEvent(event)) {
            val pkg = event.packageName ?: continue
            if (pkg !in targetSet) continue
            val eventTime = event.timeStamp

            @Suppress("DEPRECATION")
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastOpenTime[pkg] = eventTime
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    lastOpenTime[pkg]?.let { openTime ->
                        val duration = (eventTime - openTime).coerceAtLeast(0L)
                        totalUsage[pkg] = (totalUsage[pkg] ?: 0L) + duration
                    }
                    lastOpenTime.remove(pkg)
                }
                else -> { }
            }
        }

        // CRITICAL: packages still in lastOpenTime are OPEN RIGHT NOW
        for ((pkg, openTime) in lastOpenTime) {
            val liveSegment = (endTime - openTime).coerceAtLeast(0L)
            totalUsage[pkg] = (totalUsage[pkg] ?: 0L) + liveSegment
        }

        return totalUsage
    }
}
