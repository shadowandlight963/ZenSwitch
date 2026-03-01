package com.example.ourmajor.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val activityName: String,
    val category: String,
    val durationSeconds: Int,
    val timestamp: Long,
    val pointsEarned: Int,
    val isDirty: Boolean = true,
    val updatedAtMillis: Long = timestamp,
    val remoteUpdatedAtMillis: Long = 0L,
    val syncedAtMillis: Long = 0L
)
