package com.example.ourmajor.data.mindfulness

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gratitude_entries")
data class GratitudeEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAtMillis: Long
)
