package com.example.ourmajor.data.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyStats(entity: DailyStatsEntity): Long

    @Query(
        """
        UPDATE daily_stats
        SET totalMinutes = totalMinutes + :minutes,
            sessionsCount = sessionsCount + 1,
            pointsEarned = pointsEarned + :points,
            streakKept = :streakKept
        WHERE date = :date
        """
    )
    suspend fun incrementDailyStats(
        date: String,
        minutes: Int,
        points: Int,
        streakKept: Boolean
    ): Int

    @Transaction
    suspend fun upsertDailyStats(
        date: String,
        minutes: Int,
        points: Int,
        streakKept: Boolean
    ) {
        val updated = incrementDailyStats(date, minutes, points, streakKept)
        if (updated == 0) {
            insertDailyStats(
                DailyStatsEntity(
                    date = date,
                    totalMinutes = minutes,
                    sessionsCount = 1,
                    pointsEarned = points,
                    streakKept = streakKept
                )
            )
        }
    }

    @Transaction
    suspend fun updateDailyStats(date: String, minutes: Int, points: Int, streakKept: Boolean) {
        upsertDailyStats(date = date, minutes = minutes, points = points, streakKept = streakKept)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query(
        """
        SELECT * FROM daily_stats
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date ASC
        """
    )
    fun getWeekStats(startDate: String, endDate: String): Flow<List<DailyStatsEntity>>

    @Query(
        """
        SELECT * FROM daily_stats
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date ASC
        """
    )
    suspend fun getWeekStatsOnce(startDate: String, endDate: String): List<DailyStatsEntity>

    @Query(
        """
        SELECT COALESCE(SUM(totalMinutes), 0) FROM daily_stats
        """
    )
    fun observeTotalMinutes(): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(pointsEarned), 0) FROM daily_stats
        """
    )
    fun observeTotalPoints(): Flow<Long>

    @Query(
        """
        SELECT
            COALESCE(SUM(totalMinutes), 0) AS totalMinutes,
            COALESCE(SUM(pointsEarned), 0) AS totalPoints
        FROM daily_stats
        """
    )
    fun observeTotals(): Flow<TotalStats>

    @Query(
        """
        SELECT
            COALESCE(SUM(totalMinutes), 0) AS totalMinutes,
            COALESCE(SUM(pointsEarned), 0) AS totalPoints
        FROM daily_stats
        """
    )
    suspend fun getTotalStatsOnce(): TotalStats

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<SessionEntity>>
}

data class TotalStats(
    val totalMinutes: Long,
    val totalPoints: Long
)
