package com.example.ourmajor.data.stats

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: String,
    val totalMinutes: Int,
    val sessionsCount: Int,
    val pointsEarned: Int,
    val streakKept: Boolean
)
