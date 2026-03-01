package com.example.ourmajor.data.history

data class Session(
    val id: String,
    val activityName: String,
    val category: String,
    val durationSeconds: Int,
    val timestamp: Long,
    val pointsEarned: Int
)
