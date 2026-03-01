package com.example.ourmajor.data.catalog

data class ActivityCategory(
    val id: String,
    val name: String,
    val iconRes: String,
    val colorRes: String,
    val description: String
)

data class Activity(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val iconRes: String,
    val audioRes: String? = null,
    val isPremium: Boolean = false
)
