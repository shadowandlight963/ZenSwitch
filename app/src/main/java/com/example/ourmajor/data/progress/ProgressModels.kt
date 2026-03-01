package com.example.ourmajor.data.progress

import java.time.LocalDate

data class ActivitySession(
    val id: String,
    val title: String,
    val category: String,
    val minutes: Int,
    val completed: Boolean,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val dayId: String
)

data class DailyStats(
    val dayId: String,
    val totalMinutes: Int,
    val totalSessions: Int,
    val completedSessions: Int,
    val lastSessionAtMillis: Long?
)

data class UserSummary(
    val uid: String,
    val email: String?,
    val points: Int,
    val totalMinutes: Int,
    val totalSessions: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val lastActiveDayId: String?
)

fun LocalDate.toDayId(): String = toString()
