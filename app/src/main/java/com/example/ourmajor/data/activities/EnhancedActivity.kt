package com.example.ourmajor.data.activities

data class MainCategory(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val icon: String
)

data class SubActivity(
    val id: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val defaultTimeMinutes: Int,
    val instructions: String,
    val isCompleted: Boolean = false,
    val completionCount: Int = 0
)

data class ActivityWithTime(
    val activity: SubActivity,
    val selectedTimeMinutes: Int
)
