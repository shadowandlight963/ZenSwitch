package com.example.ourmajor.data.stretching

data class StretchStep(
    val name: String,
    val durationSeconds: Int,
    val instruction: String,
    val isRest: Boolean
)

data class StretchingRoutine(
    val id: String,
    val title: String,
    val steps: List<StretchStep>
) {
    val totalDurationSeconds: Int = steps.sumOf { it.durationSeconds }
}

object StretchingRoutines {

    const val ROUTINE_NECK_RELIEF = "neck_relief"
    const val ROUTINE_SPINE_MOBILITY = "spine_mobility"
    const val ROUTINE_FULL_BODY_UNWIND = "full_body_unwind"

    fun getRoutine(id: String): StretchingRoutine? {
        return when (id) {
            ROUTINE_NECK_RELIEF -> neckRelief()
            ROUTINE_SPINE_MOBILITY -> spineMobility()
            ROUTINE_FULL_BODY_UNWIND -> fullBodyUnwind()
            else -> null
        }
    }

    private fun neckRelief(): StretchingRoutine {
        return StretchingRoutine(
            id = ROUTINE_NECK_RELIEF,
            title = "Neck Relief",
            steps = listOf(
                StretchStep(
                    name = "Left Side Tilt",
                    durationSeconds = 20,
                    instruction = "Gently lower your left ear toward your left shoulder. Keep shoulders relaxed.",
                    isRest = false
                ),
                StretchStep(
                    name = "Center Rest",
                    durationSeconds = 5,
                    instruction = "Return to center and breathe.",
                    isRest = true
                ),
                StretchStep(
                    name = "Right Side Tilt",
                    durationSeconds = 20,
                    instruction = "Gently lower your right ear toward your right shoulder. Keep shoulders relaxed.",
                    isRest = false
                ),
                StretchStep(
                    name = "Center Rest",
                    durationSeconds = 5,
                    instruction = "Return to center and breathe.",
                    isRest = true
                ),
                StretchStep(
                    name = "Chin to Chest",
                    durationSeconds = 20,
                    instruction = "Slowly lower your chin toward your chest. Keep the neck long.",
                    isRest = false
                ),
                StretchStep(
                    name = "Slow Rotation",
                    durationSeconds = 30,
                    instruction = "Slowly circle the head in a comfortable range. Stop if you feel strain.",
                    isRest = false
                )
            )
        )
    }

    private fun spineMobility(): StretchingRoutine {
        return StretchingRoutine(
            id = ROUTINE_SPINE_MOBILITY,
            title = "Spine Mobility",
            steps = listOf(
                StretchStep(
                    name = "Seated Twist Left",
                    durationSeconds = 30,
                    instruction = "Sit tall. Gently twist to the left, keeping hips grounded.",
                    isRest = false
                ),
                StretchStep(
                    name = "Rest",
                    durationSeconds = 5,
                    instruction = "Return to center and breathe.",
                    isRest = true
                ),
                StretchStep(
                    name = "Seated Twist Right",
                    durationSeconds = 30,
                    instruction = "Sit tall. Gently twist to the right, keeping hips grounded.",
                    isRest = false
                ),
                StretchStep(
                    name = "Rest",
                    durationSeconds = 5,
                    instruction = "Return to center and breathe.",
                    isRest = true
                ),
                StretchStep(
                    name = "Seated Cat / Cow",
                    durationSeconds = 40,
                    instruction = "Round the spine (cat), then lift the chest (cow) slowly with your breath.",
                    isRest = false
                )
            )
        )
    }

    private fun fullBodyUnwind(): StretchingRoutine {
        return StretchingRoutine(
            id = ROUTINE_FULL_BODY_UNWIND,
            title = "Full Body Unwind",
            steps = listOf(
                StretchStep(
                    name = "Overhead Reach",
                    durationSeconds = 30,
                    instruction = "Reach both arms overhead. Lengthen through your sides.",
                    isRest = false
                ),
                StretchStep(
                    name = "Forward Fold",
                    durationSeconds = 30,
                    instruction = "Hinge at the hips and fold forward. Keep knees soft.",
                    isRest = false
                ),
                StretchStep(
                    name = "Shoulder Shrugs",
                    durationSeconds = 30,
                    instruction = "Lift shoulders up toward ears, then relax them down slowly.",
                    isRest = false
                ),
                StretchStep(
                    name = "Deep Rest",
                    durationSeconds = 30,
                    instruction = "Stand or sit comfortably. Relax the jaw and breathe.",
                    isRest = true
                )
            )
        )
    }
}
