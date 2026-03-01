package com.example.ourmajor.ui.activities

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ourmajor.ui.breathing.BoxBreathingActivity
import com.example.ourmajor.ui.breathing.ResonantBreathActivity
import com.example.ourmajor.ui.breathing.SleepBreathActivity
import com.example.ourmajor.ui.journaling.JournalingPlaceholder1Activity
import com.example.ourmajor.ui.journaling.JournalingPlaceholder2Activity
import com.example.ourmajor.ui.journaling.JournalingSessionActivity
import com.example.ourmajor.ui.mindfulness.MindfulnessPlaceholder1Activity
import com.example.ourmajor.ui.mindfulness.MindfulnessPlaceholder2Activity
import com.example.ourmajor.ui.mindfulness.MindfulnessSessionActivity
import com.example.ourmajor.ui.quickgames.GamePlaceholder1Activity
import com.example.ourmajor.ui.quickgames.GamePlaceholder2Activity
import com.example.ourmajor.ui.quickgames.QuickGamesActivity
import com.example.ourmajor.ui.stretching.StretchPlaceholder1Activity
import com.example.ourmajor.ui.stretching.StretchPlaceholder2Activity
import com.example.ourmajor.ui.stretching.StretchingSessionActivity
import kotlin.random.Random

/**
 * Smart Shuffle engine for deterministic activity selection.
 * 
 * Guarantees no repetition until all 15 activities are used.
 * Maintains shuffled order and cycle persistence.
 */
class SmartShuffleEngine {
    
    companion object {
        private const val TAG = "SmartShuffleEngine"
        
        // Complete list of 15 activities
        private val ALL_ACTIVITIES = listOf(
            ActivityInfo("Box Breathing", "breathing") { ctx -> BoxBreathingActivity.start(ctx, 2) },
            ActivityInfo("Sleep Breathing", "breathing") { ctx -> SleepBreathActivity.start(ctx, 1) },
            ActivityInfo("Resonant Wave", "breathing") { ctx -> ResonantBreathActivity.start(ctx, 1) },
            ActivityInfo("Neck Relief", "stretching") { ctx -> 
                ctx.startActivity(Intent(ctx, StretchPlaceholder1Activity::class.java))
            },
            ActivityInfo("Spine Mobility", "stretching") { ctx -> 
                ctx.startActivity(Intent(ctx, StretchPlaceholder2Activity::class.java))
            },
            ActivityInfo("Full Body Unwind", "stretching") { ctx -> StretchingSessionActivity.start(ctx, "full_body") },
            ActivityInfo("Gratitude Moment", "mindfulness") { ctx -> 
                ctx.startActivity(Intent(ctx, MindfulnessPlaceholder1Activity::class.java))
            },
            ActivityInfo("Set Intention", "mindfulness") { ctx -> 
                ctx.startActivity(Intent(ctx, MindfulnessPlaceholder2Activity::class.java))
            },
            ActivityInfo("Mindful Pause", "mindfulness") { ctx -> 
                ctx.startActivity(Intent(ctx, MindfulnessSessionActivity::class.java))
            },
            ActivityInfo("Quick Reflection", "journaling") { ctx -> 
                ctx.startActivity(Intent(ctx, JournalingPlaceholder1Activity::class.java))
            },
            ActivityInfo("Priority Check", "journaling") { ctx -> 
                ctx.startActivity(Intent(ctx, JournalingPlaceholder2Activity::class.java))
            },
            ActivityInfo("Energy Audit", "journaling") { ctx -> 
                ctx.startActivity(Intent(ctx, JournalingSessionActivity::class.java))
            },
            ActivityInfo("Memory Matrix", "quickgames") { ctx -> 
                ctx.startActivity(Intent(ctx, GamePlaceholder1Activity::class.java))
            },
            ActivityInfo("Bubble Focus", "quickgames") { ctx -> 
                ctx.startActivity(Intent(ctx, GamePlaceholder2Activity::class.java))
            },
            ActivityInfo("Zen Scramble", "quickgames") { ctx -> 
                ctx.startActivity(Intent(ctx, QuickGamesActivity::class.java))
            }
        )
    }
    
    // Instance state
    private var shuffledList: List<ActivityInfo> = emptyList()
    private var currentIndex: Int = 0
    
    /**
     * Initialize or reinitialize the shuffled list.
     * Called when engine is created or when cycle completes.
     */
    private fun reshuffle() {
        shuffledList = ALL_ACTIVITIES.shuffled(Random(System.currentTimeMillis()))
        currentIndex = 0
    }
    
    /**
     * Refresh the shuffle list.
     */
    fun refresh() {
        shuffledList = ALL_ACTIVITIES.shuffled()
        currentIndex = 0
    }
    
    /**
     * Get the next activity from the shuffled list.
     * Automatically reshuffles when all activities are used.
     */
    fun getNextActivity(): ActivityInfo {
        // Initialize on first use if needed
        if (shuffledList.isEmpty()) {
            reshuffle()
        }
        
        // Check if we need to reshuffle (cycle complete)
        if (currentIndex >= shuffledList.size) {
            reshuffle()
        }
        
        val activity = shuffledList[currentIndex]
        currentIndex++
        return activity
    }
    
    /**
     * Get current position in cycle (1-15).
     */
    fun getCurrentPosition(): Int = if (currentIndex == 0) 1 else currentIndex
    
    /**
     * Get total activities in cycle.
     */
    fun getTotalActivities(): Int = ALL_ACTIVITIES.size
    
    /**
     * Check if we're at the start of a new cycle.
     */
    fun isNewCycle(): Boolean = currentIndex == 1 || shuffledList.isEmpty()
}

/**
 * Data class representing an activity with its launch logic.
 */
data class ActivityInfo(
    val title: String,
    val category: String,
    val launchAction: (Context) -> Unit
) {
    fun launchWithLogging(context: Context) {
        val activityName = when {
            title.contains("Box Breathing") -> "BoxBreathingActivity"
            title.contains("Sleep Breathing") -> "SleepBreathActivity"
            title.contains("Resonant Wave") -> "ResonantBreathActivity"
            title.contains("Neck Relief") -> "StretchPlaceholder1Activity"
            title.contains("Spine Mobility") -> "StretchPlaceholder2Activity"
            title.contains("Full Body Unwind") -> "StretchingSessionActivity"
            title.contains("Gratitude Moment") -> "MindfulnessPlaceholder1Activity"
            title.contains("Set Intention") -> "MindfulnessPlaceholder2Activity"
            title.contains("Mindful Pause") -> "MindfulnessSessionActivity"
            title.contains("Quick Reflection") -> "JournalingPlaceholder1Activity"
            title.contains("Priority Check") -> "JournalingPlaceholder2Activity"
            title.contains("Energy Audit") -> "JournalingSessionActivity"
            title.contains("Memory Matrix") -> "GamePlaceholder1Activity"
            title.contains("Bubble Focus") -> "GamePlaceholder2Activity"
            title.contains("Zen Scramble") -> "QuickGamesActivity"
            else -> "UnknownActivity"
        }
        
        Log.d("SHUFFLE_DEBUG", "Launching: $activityName")
        Log.d("SHUFFLE_DEBUG", "Context type: ${context.javaClass.simpleName}")
        Log.d("SHUFFLE_DEBUG", "Launch action: $title")
        
        try {
            launchAction(context)
            Log.d("SHUFFLE_DEBUG", "Successfully launched: $activityName")
        } catch (e: Exception) {
            Log.e("SHUFFLE_DEBUG", "Failed to launch $activityName", e)
        }
    }
}
