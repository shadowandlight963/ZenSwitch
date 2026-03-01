package com.example.ourmajor.data.activities

class EnhancedActivitiesRepository {
    
    private val mainCategories = listOf(
        MainCategory(
            id = "breathing",
            title = "Breathing",
            description = "Mindful breathing exercises for relaxation and focus",
            order = 1,
            icon = "\ud83e\uddd8"
        ),
        MainCategory(
            id = "stretching",
            title = "Stretching", 
            description = "Gentle stretches to release tension and improve flexibility",
            order = 2,
            icon = "\ud83e\udd38"
        ),
        MainCategory(
            id = "mindfulness",
            title = "Mindfulness",
            description = "Present moment awareness and mental clarity exercises",
            order = 3,
            icon = "\ud83e\uddd8\u200d\u2640\ufe0f"
        ),
        MainCategory(
            id = "journaling",
            title = "Journaling",
            description = "Reflective writing prompts for self-discovery",
            order = 4,
            icon = "\ud83d\udcdd"
        ),
        MainCategory(
            id = "quick_games",
            title = "Quick Games",
            description = "Engaging mental exercises for quick refresh",
            order = 5,
            icon = "\ud83c\udfae"
        )
    )
    
    private val subActivities = listOf(
        // Breathing Sub-Activities
        SubActivity(
            id = "box_breathing",
            categoryId = "breathing",
            title = "Box Breathing",
            description = "4-4-4-4 breathing pattern for focus and calm",
            durationMinutes = 2,
            defaultTimeMinutes = 2,
            instructions = "Inhale for 4 counts, hold for 4, exhale for 4, hold for 4. Repeat."
        ),
        SubActivity(
            id = "deep_breathing",
            categoryId = "breathing", 
            title = "4-7-8 Sleep Breath",
            description = "Natural tranquilizer for the nervous system. 4s In, 7s Hold, 8s Out.",
            durationMinutes = 3,
            defaultTimeMinutes = 3,
            instructions = "Inhale for 4 seconds, hold for 7 seconds, exhale for 8 seconds. Repeat until the timer ends."
        ),
        SubActivity(
            id = "calm_breath",
            categoryId = "breathing",
            title = "Resonant Wave",
            description = "Balance your nervous system with smooth, continuous breathing. 6s In, 6s Out.",
            durationMinutes = 1,
            defaultTimeMinutes = 1,
            instructions = "Inhale for 6 seconds, exhale for 6 seconds. Keep the breath smooth and continuous until the timer ends."
        ),
        
        // Stretching Sub-Activities
        SubActivity(
            id = "neck_stretching",
            categoryId = "stretching",
            title = "Neck Relief",
            description = "Quick sequence to release neck tension",
            durationMinutes = 2,
            defaultTimeMinutes = 2,
            instructions = "Follow the step-by-step sequence: side tilts, rests, chin to chest, and a slow rotation."
        ),
        SubActivity(
            id = "core_stretching",
            categoryId = "stretching",
            title = "Spine Mobility",
            description = "Gentle twists and seated cat/cow to loosen your spine",
            durationMinutes = 3,
            defaultTimeMinutes = 3,
            instructions = "Move through twists with brief rests, then finish with seated cat/cow."
        ),
        SubActivity(
            id = "relaxation_stretching",
            categoryId = "stretching",
            title = "Full Body Unwind",
            description = "Unwind head-to-toe with a calming flow",
            durationMinutes = 5,
            defaultTimeMinutes = 5,
            instructions = "A calming sequence: overhead reach, forward fold, shoulder shrugs, and deep rest."
        ),
        
        // Mindfulness Sub-Activities
        SubActivity(
            id = "gratitude_moment",
            categoryId = "mindfulness",
            title = "Gratitude Moment",
            description = "Focus on what you're grateful for right now",
            durationMinutes = 2,
            defaultTimeMinutes = 2,
            instructions = "Close eyes and think of 3 things you're grateful for. Feel the gratitude."
        ),
        SubActivity(
            id = "set_intention",
            categoryId = "mindfulness",
            title = "Set Intention",
            description = "Set a clear intention for your day or task",
            durationMinutes = 1,
            defaultTimeMinutes = 1,
            instructions = "Take a moment to set a clear, positive intention for what's ahead."
        ),
        SubActivity(
            id = "mindful_pause",
            categoryId = "mindfulness",
            title = "Mindful Pause",
            description = "Brief moment of present-moment awareness",
            durationMinutes = 1,
            defaultTimeMinutes = 1,
            instructions = "Pause, notice your breath, and be fully present for this moment."
        ),
        
        // Journaling Sub-Activities
        SubActivity(
            id = "quick_reflection",
            categoryId = "journaling",
            title = "Quick Reflection",
            description = "Brief written reflection on current state",
            durationMinutes = 3,
            defaultTimeMinutes = 3,
            instructions = "Write freely about how you're feeling right now without judgment."
        ),
        SubActivity(
            id = "priority_check",
            categoryId = "journaling",
            title = "Priority Check",
            description = "Identify and clarify your current priorities",
            durationMinutes = 2,
            defaultTimeMinutes = 2,
            instructions = "Write down your top 3 priorities for today or this week."
        ),
        SubActivity(
            id = "energy_audit",
            categoryId = "journaling",
            title = "Energy Audit",
            description = "Assess your energy levels and sources",
            durationMinutes = 3,
            defaultTimeMinutes = 3,
            instructions = "Note what drains your energy and what gives you energy."
        ),
        
        // Quick Games Sub-Activities
        SubActivity(
            id = "memory_matrix",
            categoryId = "quick_games",
            title = "Memory Matrix",
            description = "Remember the flashing tiles and repeat the sequence",
            durationMinutes = 2,
            defaultTimeMinutes = 2,
            instructions = "Watch the flashing tiles, then tap them back in the same order."
        ),
        SubActivity(
            id = "bubble_focus",
            categoryId = "quick_games",
            title = "Bubble Focus",
            description = "Pop bubbles in order from 1 to 10",
            durationMinutes = 1,
            defaultTimeMinutes = 1,
            instructions = "Tap bubble 1, then 2, and so on until you pop 10."
        ),
        SubActivity(
            id = "zen_scramble",
            categoryId = "quick_games",
            title = "Zen Scramble",
            description = "Unscramble calming words letter by letter",
            durationMinutes = 2,
            defaultTimeMinutes = 2,
            instructions = "Tap letters to fill the slots and form the hidden word."
        )
    )
    
    fun getMainCategories(): List<MainCategory> = mainCategories.sortedBy { it.order }
    
    fun getSubActivitiesByCategory(categoryId: String): List<SubActivity> {
        return subActivities.filter { it.categoryId == categoryId }
    }
    
    fun getAllSubActivities(): List<SubActivity> = subActivities
    
    fun getSubActivityById(id: String): SubActivity? {
        return subActivities.find { it.id == id }
    }
    
    fun getFeaturedActivities(): List<SubActivity> {
        return listOfNotNull(
            subActivities.find { it.id == "box_breathing" },
            subActivities.find { it.id == "deep_breathing" },
            subActivities.find { it.id == "neck_stretching" },
            subActivities.find { it.id == "core_stretching" },
            subActivities.find { it.id == "gratitude_moment" },
            subActivities.find { it.id == "mindful_pause" },
            subActivities.find { it.id == "quick_reflection" },
            subActivities.find { it.id == "priority_check" },
            subActivities.find { it.id == "memory_matrix" },
            subActivities.find { it.id == "bubble_focus" }
        )
    }
}
