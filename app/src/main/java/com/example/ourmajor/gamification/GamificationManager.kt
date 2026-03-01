package com.example.ourmajor.gamification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ourmajor.data.history.SessionHistoryRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

private val Context.gamificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "gamification")

enum class ActivityType {
    BREATHING,
    STRETCHING,
    JOURNALING,
    GAMES,
    MINDFULNESS
}

enum class BadgeId(val title: String) {
    EARLY_BIRD("Early Bird"),
    STREAK_MASTER("Streak Master"),
    ZEN_WARRIOR("Zen Warrior"),
    FOCUS_NINJA("Focus Ninja")
}

data class RewardResult(
    val activityType: ActivityType,
    val basePoints: Int,
    val pointsEarned: Int,
    val multipliers: List<String>,
    val isEarlyBird: Boolean,
    val isStreakBonus: Boolean,
    val oldTotalPoints: Int,
    val newTotalPoints: Int,
    val oldLevel: Int,
    val newLevel: Int,
    val isLevelUp: Boolean,
    val newBadges: List<BadgeId>
)

data class StoreItem(
    val id: String,
    val title: String,
    val cost: Int,
    val description: String
)

data class GamificationState(
    val totalPoints: Int,
    val currentLevel: Int,
    val currentStreak: Int,
    val unlockedThemes: Set<String>,
    val streakFreezeUntilMs: Long,
    val earnedBadges: Set<String>
)

sealed class GamificationEvent {
    data class RewardAwarded(val result: RewardResult) : GamificationEvent()
    data class LevelUp(val newLevel: Int) : GamificationEvent()
    data class BadgeUnlocked(val badge: BadgeId) : GamificationEvent()
    data class PurchaseSuccess(val itemId: String) : GamificationEvent()
    data class PurchaseFailed(val reason: String) : GamificationEvent()
}

class GamificationManager private constructor(
    private val appContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ds = appContext.gamificationDataStore

    private val _events = MutableSharedFlow<GamificationEvent>(extraBufferCapacity = 32)
    val events: Flow<GamificationEvent> = _events

    val state: StateFlow<GamificationState> = ds.data
        .map { p ->
            GamificationState(
                totalPoints = p[KEY_TOTAL_POINTS] ?: 0,
                currentLevel = p[KEY_CURRENT_LEVEL] ?: 0,
                currentStreak = p[KEY_CURRENT_STREAK] ?: 0,
                unlockedThemes = p[KEY_UNLOCKED_THEMES] ?: emptySet(),
                streakFreezeUntilMs = p[KEY_STREAK_FREEZE_UNTIL] ?: 0L,
                earnedBadges = p[KEY_EARNED_BADGES] ?: emptySet()
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, GamificationState(0, 0, 0, emptySet(), 0L, emptySet()))

    suspend fun grantPoints(points: Int) {
        val delta = points.coerceAtLeast(0)
        if (delta == 0) return
        ds.edit { p ->
            val newTotal = (p[KEY_TOTAL_POINTS] ?: 0) + delta
            p[KEY_TOTAL_POINTS] = newTotal
            p[KEY_CURRENT_LEVEL] = newTotal / 500
        }
    }

    val storeItems: List<StoreItem> = listOf(
        StoreItem(
            id = ITEM_THEME_OCEAN,
            title = "Ocean Breeze Theme",
            cost = 500,
            description = "Unlock an ocean-inspired theme"
        ),
        StoreItem(
            id = ITEM_THEME_SUNSET,
            title = "Sunset Serenity Theme",
            cost = 500,
            description = "Unlock a sunset-inspired theme"
        ),
        StoreItem(
            id = ITEM_THEME_MINIMAL_MIST,
            title = "Minimalist Mist",
            cost = 500,
            description = "A clean, foggy grey aesthetic."
        ),
        StoreItem(
            id = ITEM_STREAK_FREEZE,
            title = "Streak Freeze",
            cost = 1000,
            description = "Protect your streak for 24h"
        )
    )

    suspend fun awardPoints(
        activityType: ActivityType,
        duration: Int,
        gameScore: Int = 0,
        completedAtMillis: Long = System.currentTimeMillis(),
        activityName: String? = null,
        category: String? = null
    ): RewardResult {
        val localTime = Instant.ofEpochMilli(completedAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val isEarlyBird = localTime.isBefore(LocalTime.of(8, 0))

        val current = state.first()

        val today = Instant.ofEpochMilli(completedAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val lastActiveDayId = ds.data.first()[KEY_LAST_ACTIVE_DAY_ID]
        val freezeUntil = current.streakFreezeUntilMs
        val newStreak = computeNewStreak(lastActiveDayId, today.toString(), current.currentStreak, freezeUntil, completedAtMillis)

        val base = when (activityType) {
            ActivityType.BREATHING -> (duration.coerceAtLeast(0) * 10)
            ActivityType.STRETCHING -> 15
            ActivityType.JOURNALING -> 20
            ActivityType.GAMES -> 5
            ActivityType.MINDFULNESS -> (duration.coerceAtLeast(0) * 10)
        }

        var multiplier = 1.0
        val multipliers = ArrayList<String>(2)
        if (isEarlyBird) {
            multiplier *= 1.5
            multipliers.add("Early Bird x1.5")
        }
        val isStreakBonus = newStreak > 7
        if (isStreakBonus) {
            multiplier *= 2.0
            multipliers.add("Streak Bonus x2")
        }

        val earned = kotlin.math.round(base * multiplier).toInt().coerceAtLeast(0)
        val oldTotal = current.totalPoints
        val newTotal = oldTotal + earned

        val oldLevel = current.currentLevel
        val newLevel = newTotal / 500
        val isLevelUp = newLevel > oldLevel

        val earnedBadgesBefore = current.earnedBadges
        val newlyEarned = ArrayList<BadgeId>()

        fun maybeUnlock(b: BadgeId, condition: Boolean) {
            if (!condition) return
            if (earnedBadgesBefore.contains(b.name)) return
            newlyEarned.add(b)
        }

        maybeUnlock(BadgeId.EARLY_BIRD, isEarlyBird)
        maybeUnlock(BadgeId.STREAK_MASTER, newStreak >= 7)
        maybeUnlock(BadgeId.ZEN_WARRIOR, newTotal >= 1000)
        maybeUnlock(BadgeId.FOCUS_NINJA, activityType == ActivityType.GAMES && gameScore > 50)

        val willConsumeFreeze = shouldConsumeFreeze(lastActiveDayId, today.toString(), freezeUntil, completedAtMillis)

        ds.edit { p ->
            p[KEY_TOTAL_POINTS] = newTotal
            p[KEY_CURRENT_LEVEL] = newLevel
            p[KEY_CURRENT_STREAK] = newStreak
            p[KEY_LAST_ACTIVE_DAY_ID] = today.toString()
            if (willConsumeFreeze) {
                p[KEY_STREAK_FREEZE_UNTIL] = 0L
            }

            if (newlyEarned.isNotEmpty()) {
                val updated = (p[KEY_EARNED_BADGES] ?: emptySet()).toMutableSet()
                newlyEarned.forEach { updated.add(it.name) }
                p[KEY_EARNED_BADGES] = updated
            }
        }

        val result = RewardResult(
            activityType = activityType,
            basePoints = base,
            pointsEarned = earned,
            multipliers = multipliers,
            isEarlyBird = isEarlyBird,
            isStreakBonus = isStreakBonus,
            oldTotalPoints = oldTotal,
            newTotalPoints = newTotal,
            oldLevel = oldLevel,
            newLevel = newLevel,
            isLevelUp = isLevelUp,
            newBadges = newlyEarned
        )

        runCatching {
            val resolvedName = activityName ?: when (activityType) {
                ActivityType.BREATHING -> "Breathing"
                ActivityType.STRETCHING -> "Stretching"
                ActivityType.JOURNALING -> "Journaling"
                ActivityType.GAMES -> "Games"
                ActivityType.MINDFULNESS -> "Mindfulness"
            }
            val resolvedCategory = category ?: when (activityType) {
                ActivityType.BREATHING -> "Breathing"
                ActivityType.STRETCHING -> "Stretching"
                ActivityType.JOURNALING -> "Journaling"
                ActivityType.GAMES -> "Games"
                ActivityType.MINDFULNESS -> "Mindfulness"
            }
            val durationSeconds = (duration.coerceAtLeast(0) * 60)
            val history = SessionHistoryRepository(appContext)
            withContext(Dispatchers.IO) {
                history.recordSession(
                    id = UUID.randomUUID().toString(),
                    activityName = resolvedName,
                    category = resolvedCategory,
                    durationSeconds = durationSeconds,
                    timestamp = completedAtMillis,
                    pointsEarned = earned
                )
            }
        }

        scope.launch {
            _events.tryEmit(GamificationEvent.RewardAwarded(result))
            if (isLevelUp) {
                _events.tryEmit(GamificationEvent.LevelUp(newLevel))
            }
            newlyEarned.forEach { b ->
                _events.tryEmit(GamificationEvent.BadgeUnlocked(b))
            }
        }

        return result
    }

    suspend fun purchaseItem(itemId: String): Boolean {
        val cur = state.first()
        val item = storeItems.firstOrNull { it.id == itemId }
        if (item == null) {
            _events.tryEmit(GamificationEvent.PurchaseFailed("Unknown item"))
            return false
        }
        if (cur.totalPoints < item.cost) {
            _events.tryEmit(GamificationEvent.PurchaseFailed("Not enough points"))
            return false
        }

        ds.edit { p ->
            val newTotal = (p[KEY_TOTAL_POINTS] ?: 0) - item.cost
            p[KEY_TOTAL_POINTS] = newTotal.coerceAtLeast(0)

            when (itemId) {
                ITEM_THEME_OCEAN, ITEM_THEME_SUNSET, ITEM_THEME_MINIMAL_MIST -> {
                    val themes = (p[KEY_UNLOCKED_THEMES] ?: emptySet()).toMutableSet()
                    themes.add(itemId)
                    p[KEY_UNLOCKED_THEMES] = themes
                }
                ITEM_STREAK_FREEZE -> {
                    p[KEY_STREAK_FREEZE_UNTIL] = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
                }
            }
        }

        _events.tryEmit(GamificationEvent.PurchaseSuccess(itemId))
        return true
    }

    fun isThemeUnlocked(state: GamificationState, themeItemId: String): Boolean {
        return state.unlockedThemes.contains(themeItemId)
    }

    companion object {
        private val KEY_TOTAL_POINTS = intPreferencesKey("total_points")
        private val KEY_CURRENT_LEVEL = intPreferencesKey("current_level")
        private val KEY_CURRENT_STREAK = intPreferencesKey("current_streak")
        private val KEY_LAST_ACTIVE_DAY_ID = stringPreferencesKey("last_active_day_id")
        private val KEY_STREAK_FREEZE_UNTIL = longPreferencesKey("streak_freeze_until")
        private val KEY_UNLOCKED_THEMES = stringSetPreferencesKey("unlocked_themes")
        private val KEY_EARNED_BADGES = stringSetPreferencesKey("earned_badges")

        const val ITEM_THEME_OCEAN = "theme_ocean_breeze"
        const val ITEM_THEME_SUNSET = "theme_sunset_serenity"
        const val ITEM_THEME_MINIMAL_MIST = "theme_minimalist_mist"
        const val ITEM_STREAK_FREEZE = "streak_freeze"

        @Volatile
        private var instance: GamificationManager? = null

        fun getInstance(context: Context): GamificationManager {
            return instance ?: synchronized(this) {
                instance ?: GamificationManager(context.applicationContext).also { instance = it }
            }
        }

        private fun computeNewStreak(
            lastActiveDayId: String?,
            newDayId: String,
            currentStreak: Int,
            streakFreezeUntilMs: Long,
            nowMillis: Long
        ): Int {
            if (lastActiveDayId.isNullOrBlank()) return 1
            if (lastActiveDayId == newDayId) return currentStreak.coerceAtLeast(1)

            val last = runCatching { LocalDate.parse(lastActiveDayId) }.getOrNull() ?: return 1
            val newDay = runCatching { LocalDate.parse(newDayId) }.getOrNull() ?: return 1

            val expectedNext = last.plusDays(1)
            if (newDay == expectedNext) return currentStreak + 1

            val missedOneDay = newDay == last.plusDays(2)
            val freezeActive = streakFreezeUntilMs > nowMillis
            return if (missedOneDay && freezeActive) {
                currentStreak + 1
            } else {
                1
            }
        }

        private fun shouldConsumeFreeze(
            lastActiveDayId: String?,
            newDayId: String,
            streakFreezeUntilMs: Long,
            nowMillis: Long
        ): Boolean {
            if (lastActiveDayId.isNullOrBlank()) return false
            val last = runCatching { LocalDate.parse(lastActiveDayId) }.getOrNull() ?: return false
            val newDay = runCatching { LocalDate.parse(newDayId) }.getOrNull() ?: return false
            val missedOneDay = newDay == last.plusDays(2)
            val freezeActive = streakFreezeUntilMs > nowMillis
            return missedOneDay && freezeActive
        }
    }
}
