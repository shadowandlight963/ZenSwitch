package com.example.ourmajor.data.history

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SessionHistoryRepository(context: Context) {
    private val dao = AppDatabase.get(context).sessionDao()

    suspend fun recordSession(
        id: String,
        activityName: String,
        category: String,
        durationSeconds: Int,
        timestamp: Long,
        pointsEarned: Int
    ) {
        dao.upsert(
            SessionEntity(
                id = id,
                activityName = activityName,
                category = category,
                durationSeconds = durationSeconds,
                timestamp = timestamp,
                pointsEarned = pointsEarned
            )
        )
    }

    fun observeBetween(startInclusive: Long, endInclusive: Long): Flow<List<SessionEntity>> {
        return dao.observeBetween(startInclusive, endInclusive)
    }

    fun observeRecent(limit: Int): Flow<List<SessionEntity>> {
        return dao.observeRecent(limit)
    }

    fun observeTotalCount(): Flow<Int> {
        return dao.observeTotalCount()
    }

    fun observeTotalDurationSeconds(): Flow<Long> {
        return dao.observeTotalDurationSeconds()
    }

    suspend fun getBetween(startInclusive: Long, endInclusive: Long): List<SessionEntity> {
        return dao.getBetween(startInclusive, endInclusive)
    }
}
