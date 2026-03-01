package com.example.ourmajor.data.stats

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SessionRepository(
    context: Context,
    private val dao: SessionDao = AppDatabase.getDatabase(context).sessionDao()
) {

    suspend fun logSession(
        type: String,
        duration: Int,
        timestampMillis: Long = System.currentTimeMillis(),
        streakKept: Boolean = duration > 0
    ) {
        val minutes = duration.coerceAtLeast(0)
        val points = (minutes * 5).coerceAtLeast(0)

        dao.insertSession(
            SessionEntity(
                timestamp = timestampMillis,
                durationMinutes = minutes,
                activityType = type,
                pointsEarned = points
            )
        )

        val date = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        dao.upsertDailyStats(
            date = date,
            minutes = minutes,
            points = points,
            streakKept = streakKept
        )
    }

    fun getWeeklyProgress(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyStatsEntity>> {
        return dao.getWeekStats(startDate.toString(), endDate.toString())
    }

    fun getWeeklyProgress(): Flow<List<DailyStatsEntity>> {
        val today = LocalDate.now()
        val start = today.minusDays(6)
        return getWeeklyProgress(start, today)
    }

    fun observeTotals(): Flow<TotalStats> = dao.observeTotals()

    fun observeRecentSessions(limit: Int = 50): Flow<List<SessionEntity>> = dao.observeRecentSessions(limit)
}
