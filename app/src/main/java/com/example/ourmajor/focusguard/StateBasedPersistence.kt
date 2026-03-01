package com.example.ourmajor.focusguard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * State-based persistence that survives OEM kills and device reboots.
 * 
 * Strategy: Persist intervention state so service can restore behavior
 * even if killed by aggressive OEM battery savers.
 */
object StateBasedPersistence {
    
    private const val TAG = "StateBasedPersistence"
    private const val PREFS_NAME = "focus_guard_state"
        
        // State keys
    private const val KEY_INTERVENTION_ACTIVE = "intervention_active"
    private const val KEY_LIMIT_CROSSED_TIME = "limit_crossed_time"
    private const val KEY_EMERGENCY_OVERRIDE_UNTIL = "emergency_override_until"
    private const val KEY_DAILY_LIMIT_MS = "daily_limit_ms"
    private const val KEY_LAST_DETECTION_TIME = "last_detection_time"
    private const val KEY_INTERVENTION_COUNT = "intervention_count"
    private const val KEY_EMERGENCY_OVERRIDE_COUNT = "emergency_override_count"
        
        // Defaults
    private const val DEFAULT_DAILY_LIMIT_MS = 60 * 60 * 1000L // 1 hour
    private const val MAX_EMERGENCY_OVERRIDES_PER_DAY = 3
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Intervention state management
     */
    fun setInterventionActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INTERVENTION_ACTIVE, active).apply()
        Log.i(TAG, "Intervention active: $active")
    }
    
    fun isInterventionActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INTERVENTION_ACTIVE, false)
    }
    
    /**
     * Limit crossing state
     */
    fun setLimitCrossed(context: Context, crossedTime: Long) {
        getPrefs(context).edit().putLong(KEY_LIMIT_CROSSED_TIME, crossedTime).apply()
        Log.i(TAG, "Limit crossed at: $crossedTime")
    }
    
    fun getLimitCrossedTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LIMIT_CROSSED_TIME, -1L)
    }
    
    fun hasLimitCrossedToday(context: Context): Boolean {
        val crossedTime = getLimitCrossedTime(context)
        if (crossedTime == -1L) return false
        
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % (24 * 60 * 60 * 1000))
        return crossedTime >= startOfDay
    }
    
    /**
     * Emergency override management
     */
    fun setEmergencyOverride(context: Context, untilTime: Long) {
        getPrefs(context).edit().putLong(KEY_EMERGENCY_OVERRIDE_UNTIL, untilTime).apply()
        
        // Increment override count
        val currentCount = getEmergencyOverrideCount(context)
        setEmergencyOverrideCount(context, currentCount + 1)
        
        Log.i(TAG, "Emergency override until: $untilTime")
    }
    
    fun isEmergencyOverrideActive(context: Context): Boolean {
        val untilTime = getPrefs(context).getLong(KEY_EMERGENCY_OVERRIDE_UNTIL, 0L)
        return System.currentTimeMillis() < untilTime
    }
    
    fun getEmergencyOverrideCount(context: Context): Int {
        // Check if it's a new day
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % (24 * 60 * 60 * 1000))
        val lastDetection = getPrefs(context).getLong(KEY_LAST_DETECTION_TIME, 0L)
        
        if (lastDetection < startOfDay) {
            // New day, reset count
            setEmergencyOverrideCount(context, 0)
        }
        
        return getPrefs(context).getInt(KEY_EMERGENCY_OVERRIDE_COUNT, 0)
    }
    
    private fun setEmergencyOverrideCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_EMERGENCY_OVERRIDE_COUNT, count).apply()
    }
    
    fun canUseEmergencyOverride(context: Context): Boolean {
        return getEmergencyOverrideCount(context) < MAX_EMERGENCY_OVERRIDES_PER_DAY
    }
    
    /**
     * Daily limit management
     */
    fun setDailyLimit(context: Context, limitMs: Long) {
        getPrefs(context).edit().putLong(KEY_DAILY_LIMIT_MS, limitMs).apply()
        Log.i(TAG, "Daily limit set: ${limitMs / 60_000} minutes")
    }
    
    fun getDailyLimit(context: Context): Long {
        return getPrefs(context).getLong(KEY_DAILY_LIMIT_MS, DEFAULT_DAILY_LIMIT_MS)
    }
    
    /**
     * Detection tracking
     */
    fun setLastDetectionTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_DETECTION_TIME, time).apply()
    }
    
    fun getLastDetectionTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_DETECTION_TIME, 0L)
    }
    
    /**
     * Intervention statistics
     */
    fun incrementInterventionCount(context: Context) {
        val currentCount = getInterventionCount(context)
        getPrefs(context).edit().putInt(KEY_INTERVENTION_COUNT, currentCount + 1).apply()
    }
    
    fun getInterventionCount(context: Context): Int {
        // Check if it's a new day
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % (24 * 60 * 60 * 1000))
        val lastDetection = getPrefs(context).getLong(KEY_LAST_DETECTION_TIME, 0L)
        
        if (lastDetection < startOfDay) {
            // New day, reset count
            getPrefs(context).edit().putInt(KEY_INTERVENTION_COUNT, 0).apply()
        }
        
        return getPrefs(context).getInt(KEY_INTERVENTION_COUNT, 0)
    }
    
    /**
     * State reset for new day
     */
    fun resetDailyState(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_INTERVENTION_ACTIVE, false)
            putLong(KEY_LIMIT_CROSSED_TIME, -1L)
            putLong(KEY_EMERGENCY_OVERRIDE_UNTIL, 0L)
            putInt(KEY_INTERVENTION_COUNT, 0)
            putInt(KEY_EMERGENCY_OVERRIDE_COUNT, 0)
        }.apply()
        
        Log.i(TAG, "Daily state reset")
    }
    
    /**
     * Complete state dump for debugging
     */
    fun dumpState(context: Context): String {
        val prefs = getPrefs(context)
        return """
            Focus Guard State:
            - Intervention Active: ${prefs.getBoolean(KEY_INTERVENTION_ACTIVE, false)}
            - Limit Crossed Time: ${prefs.getLong(KEY_LIMIT_CROSSED_TIME, -1L)}
            - Emergency Override Until: ${prefs.getLong(KEY_EMERGENCY_OVERRIDE_UNTIL, 0L)}
            - Daily Limit: ${prefs.getLong(KEY_DAILY_LIMIT_MS, DEFAULT_DAILY_LIMIT_MS) / 60_000} minutes
            - Last Detection: ${prefs.getLong(KEY_LAST_DETECTION_TIME, 0L)}
            - Intervention Count: ${prefs.getInt(KEY_INTERVENTION_COUNT, 0)}
            - Emergency Override Count: ${prefs.getInt(KEY_EMERGENCY_OVERRIDE_COUNT, 0)}
        """.trimIndent()
    }
}
