package com.example.ourmajor.ui.activities

import kotlin.random.Random
import com.example.ourmajor.ui.breathing.BoxBreathingActivity
import com.example.ourmajor.ui.breathing.SleepBreathActivity
import com.example.ourmajor.ui.breathing.ResonantBreathActivity

/**
 * Pure Kotlin Smart Shuffle Engine - No Android dependencies.
 * 
 * Manages shuffled list of activities and current position.
 * Thread-safe and deterministic behavior.
 */
class SmartShuffleEngineNew {
    
    companion object {
        private const val TAG = "SmartShuffleEngine"
        
        // Complete list of 3 breathing activities only
        private val ALL_ACTIVITIES = listOf(
            BoxBreathingActivity::class.java,
            SleepBreathActivity::class.java,
            ResonantBreathActivity::class.java
        )
    }
    
    // Instance state - stored inside ViewModel, not static
    private var shuffledList: List<Class<out androidx.appcompat.app.AppCompatActivity>> = emptyList()
    private var currentIndex: Int = 0
    
    /**
     * Initialize or reinitialize shuffled list.
     * Called when engine is created or when cycle completes.
     */
    private fun reshuffle() {
        shuffledList = ALL_ACTIVITIES.shuffled(Random(System.currentTimeMillis()))
        currentIndex = 0
    }
    
    /**
     * Get next activity class from shuffled list.
     * Automatically reshuffles when all activities are used.
     */
    fun getNextActivity(): Class<out androidx.appcompat.app.AppCompatActivity> {
        // Initialize on first use if needed
        if (shuffledList.isEmpty()) {
            reshuffle()
        }
        
        // Check if we need to reshuffle (cycle complete)
        if (currentIndex >= shuffledList.size) {
            reshuffle()
        }
        
        // Get current activity and increment index
        val activityClass = shuffledList[currentIndex]
        currentIndex++
        
        return activityClass
    }
    
    /**
     * Get current position in cycle (1-based for display).
     */
    fun getCurrentPosition(): Int = currentIndex.coerceAtMost(shuffledList.size)
    
    /**
     * Get total activities count.
     */
    fun getTotalActivities(): Int = 3
    
    /**
     * Check if we're at the start of a new cycle.
     */
    fun isNewCycle(): Boolean = currentIndex == 1 || shuffledList.isEmpty()
    
    /**
     * Force refresh of shuffle list.
     */
    fun refresh() {
        reshuffle()
    }
    
    /**
     * Get all activity classes for debugging.
     */
    fun getAllActivities(): List<Class<out androidx.appcompat.app.AppCompatActivity>> = ALL_ACTIVITIES
}
