package com.example.ourmajor.data.mindfulness

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GratitudeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: GratitudeEntry): Long

    @Query("SELECT * FROM gratitude_entries ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<GratitudeEntry>
}
