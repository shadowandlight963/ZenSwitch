package com.example.ourmajor.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM sessions")
    fun observeTotalDurationSeconds(): Flow<Long>

    @Query("SELECT * FROM sessions WHERE timestamp >= :startInclusive AND timestamp <= :endInclusive ORDER BY timestamp DESC")
    fun observeBetween(startInclusive: Long, endInclusive: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE timestamp >= :startInclusive AND timestamp <= :endInclusive ORDER BY timestamp DESC")
    suspend fun getBetween(startInclusive: Long, endInclusive: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE isDirty = 1 ORDER BY updatedAtMillis ASC LIMIT :limit")
    suspend fun getDirty(limit: Int): List<SessionEntity>

    @Query("UPDATE sessions SET isDirty = 0, syncedAtMillis = :syncedAtMillis, remoteUpdatedAtMillis = :remoteUpdatedAtMillis WHERE id = :id")
    suspend fun markSynced(id: String, syncedAtMillis: Long, remoteUpdatedAtMillis: Long)
}
