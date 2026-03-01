package com.example.ourmajor.engine

import android.app.Activity
import com.example.ourmajor.ui.breathing.BoxBreathingActivity
import com.example.ourmajor.ui.breathing.ResonantBreathActivity
import com.example.ourmajor.ui.breathing.SleepBreathActivity
import com.example.ourmajor.ui.journaling.JournalingSessionActivity
import com.example.ourmajor.ui.journaling.JournalingPlaceholder1Activity
import com.example.ourmajor.ui.journaling.JournalingPlaceholder2Activity
import com.example.ourmajor.ui.mindfulness.MindfulnessSessionActivity
import com.example.ourmajor.ui.mindfulness.MindfulnessPlaceholder1Activity
import com.example.ourmajor.ui.mindfulness.MindfulnessPlaceholder2Activity
import com.example.ourmajor.ui.quickgames.QuickGamesActivity
import com.example.ourmajor.ui.quickgames.GamePlaceholder1Activity
import com.example.ourmajor.ui.quickgames.GamePlaceholder2Activity
import com.example.ourmajor.ui.stretching.StretchingSessionActivity
import com.example.ourmajor.ui.stretching.StretchPlaceholder1Activity
import com.example.ourmajor.ui.stretching.StretchPlaceholder2Activity

/**
 * Registry of wellness activities for the ZenSwitch nudge interceptor.
 * Categories: Breathing, Stretching, Mindfulness, Journaling, Games.
 */
object ActivityRegistry {

    private val activities: List<Class<out Activity>> = listOf(
        // Breathing (3)
        BoxBreathingActivity::class.java,
        SleepBreathActivity::class.java,
        ResonantBreathActivity::class.java,
        // Stretching (3)
        StretchingSessionActivity::class.java,
        StretchPlaceholder1Activity::class.java,
        StretchPlaceholder2Activity::class.java,
        // Mindfulness (3)
        MindfulnessSessionActivity::class.java,
        MindfulnessPlaceholder1Activity::class.java,
        MindfulnessPlaceholder2Activity::class.java,
        // Journaling (3)
        JournalingSessionActivity::class.java,
        JournalingPlaceholder1Activity::class.java,
        JournalingPlaceholder2Activity::class.java,
        // Games (3)
        QuickGamesActivity::class.java,
        GamePlaceholder1Activity::class.java,
        GamePlaceholder2Activity::class.java
    )

    /** Returns a random activity class from the registry (redirect for nudge interceptor). */
    fun getRandomActivity(): Class<out Activity> = activities.random()
}
