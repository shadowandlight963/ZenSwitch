package com.example.ourmajor.data.activities

data class Favorite(
    val activityId: String,
    val addedAt: Long = System.currentTimeMillis()
)
