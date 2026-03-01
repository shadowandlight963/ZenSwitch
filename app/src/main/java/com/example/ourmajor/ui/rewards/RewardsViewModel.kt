package com.example.ourmajor.ui.rewards

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ourmajor.data.history.SessionHistoryRepository
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.gamification.GamificationEvent
import com.example.ourmajor.gamification.GamificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class RewardsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val totalPoints: Int = 0,
    val currentLevel: Int = 0,
    val currentStreak: Int = 0,
    val weeklyTarget: Int = 5,
    val weeklyCompleted: Int = 0,
    val oceanUnlocked: Boolean = false,
    val sunsetUnlocked: Boolean = false,
    val minimalUnlocked: Boolean = false,
    val freezeActive: Boolean = false,
    val lastToast: String? = null
) {
    val weeklyProgressPercent: Int
        get() = ((weeklyCompleted.toFloat() / weeklyTarget.coerceAtLeast(1)) * 100f).toInt().coerceIn(0, 100)

    val weeklyLabel: String
        get() = "${weeklyCompleted}/${weeklyTarget}"

    val weeklyHint: String
        get() = if (weeklyCompleted >= weeklyTarget) {
            "Challenge complete — great job!"
        } else {
            "${(weeklyTarget - weeklyCompleted)} more to complete"
        }
}

class RewardsViewModel(
    application: Application,
    private val gamification: GamificationManager = GamificationManager.getInstance(application.applicationContext),
    private val repo: ProgressRepository = ProgressRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData(RewardsUiState())
    val uiState: LiveData<RewardsUiState> = _uiState

    private val historyRepo = SessionHistoryRepository(application)

    private val rewardPrefs = application.getSharedPreferences("rewards", Context.MODE_PRIVATE)

    init {
        start()
    }

    private fun start() {
        _uiState.postValue((_uiState.value ?: RewardsUiState()).copy(isLoading = true, errorMessage = null))

        viewModelScope.launch {
            gamification.state.collectLatest { s ->
                val cur = _uiState.value ?: RewardsUiState()
                _uiState.postValue(
                    cur.copy(
                        isLoading = false,
                        totalPoints = s.totalPoints,
                        currentLevel = s.currentLevel,
                        currentStreak = s.currentStreak,
                        oceanUnlocked = s.unlockedThemes.contains(GamificationManager.ITEM_THEME_OCEAN),
                        sunsetUnlocked = s.unlockedThemes.contains(GamificationManager.ITEM_THEME_SUNSET),
                        minimalUnlocked = s.unlockedThemes.contains(GamificationManager.ITEM_THEME_MINIMAL_MIST),
                        freezeActive = s.streakFreezeUntilMs > System.currentTimeMillis(),
                        errorMessage = null
                    )
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val nextMonday = monday.plusDays(7)

            val startMs = monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endExclusiveMs = nextMonday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endInclusiveMs = endExclusiveMs - 1

            historyRepo.observeBetween(startMs, endInclusiveMs).collectLatest { sessions ->
                val completed = sessions.size.coerceAtLeast(0)
                val cur = _uiState.value ?: RewardsUiState()
                _uiState.postValue(cur.copy(isLoading = false, weeklyCompleted = completed, errorMessage = null))

                val target = cur.weeklyTarget.coerceAtLeast(1)
                if (completed >= target) {
                    maybeAwardWeeklyBonus(monday.toString())
                }
            }
        }

        viewModelScope.launch {
            gamification.events.collectLatest { event ->
                when (event) {
                    is GamificationEvent.PurchaseSuccess -> showToast("Purchase Successful!")
                    is GamificationEvent.PurchaseFailed -> showToast("Purchase Failed: ${event.reason}")
                    else -> Unit
                }
            }
        }
    }

    private suspend fun maybeAwardWeeklyBonus(weekId: String) {
        val key = "weekly_bonus_claimed_week"
        val last = rewardPrefs.getString(key, null)
        if (last == weekId) return

        rewardPrefs.edit().putString(key, weekId).apply()
        gamification.grantPoints(100)
        showToast("Weekly Challenge complete! +100 points")
    }

    fun purchaseOceanTheme() {
        purchase(GamificationManager.ITEM_THEME_OCEAN)
    }

    fun purchaseSunsetTheme() {
        purchase(GamificationManager.ITEM_THEME_SUNSET)
    }

    fun purchaseMinimalTheme() {
        purchase(GamificationManager.ITEM_THEME_MINIMAL_MIST)
    }

    fun purchaseStreakFreeze() {
        purchase(GamificationManager.ITEM_STREAK_FREEZE)
    }

    fun devUnlockAll() {
        viewModelScope.launch {
            gamification.grantPoints(2000)
            showToast("DEV MODE: 2000 Points Added! Buy whatever you want.")
        }
    }

    private fun purchase(itemId: String) {
        viewModelScope.launch {
            val ok = gamification.purchaseItem(itemId)
            if (!ok) {
                showToast("Purchase failed")
            }
        }
    }

    private fun showToast(msg: String) {
        val current = _uiState.value ?: RewardsUiState()
        _uiState.postValue(current.copy(lastToast = msg))
        viewModelScope.launch {
            delay(100)
            val updated = _uiState.value ?: return@launch
            if (updated.lastToast == msg) {
                _uiState.postValue(updated.copy(lastToast = null))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
