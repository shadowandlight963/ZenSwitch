package com.example.ourmajor.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ourmajor.common.onSuccess
import com.example.ourmajor.common.onFailure
import com.example.ourmajor.data.history.SessionHistoryRepository
import com.example.ourmajor.data.profile.ProfileRepository
import com.example.ourmajor.gamification.GamificationManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val todayMinutes: Int = 0,
    val todaySessionsCount: Int = 0,
    val todayGoalMinutes: Int = 30,
    val currentStreakDays: Int = 0,
    val weekMinutes: Int = 0,
    val weekSessions: Int = 0,
    val recentActivities: List<RecentActivity> = emptyList()
)

class HomeViewModel(
    private val history: SessionHistoryRepository,
    private val gm: GamificationManager,
    private val profileRepo: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    private var settingsReg: ListenerRegistration? = null
    private var dataJob: Job? = null
    private var streakJob: Job? = null

    init {
        start()
    }

    private fun start() {
        _uiState.postValue((_uiState.value ?: HomeUiState()).copy(isLoading = true, errorMessage = null))

        // 1. Load User Goals (Daily Goal Minutes)
        settingsReg = profileRepo.listenUserDocument { result ->
            result.onSuccess { doc ->
                val cur = _uiState.value ?: HomeUiState()
                _uiState.postValue(
                    cur.copy(
                        isLoading = cur.isLoading,
                        todayGoalMinutes = doc.preferences.dailyGoalMinutes,
                        errorMessage = null
                    )
                )
            }.onFailure { e ->
                val cur = _uiState.value ?: HomeUiState()
                _uiState.postValue(cur.copy(isLoading = false, errorMessage = e.message ?: "Failed to load settings"))
            }
        }

        // 2. Load Gamification Stats (Streak)
        streakJob?.cancel()
        streakJob = viewModelScope.launch {
            gm.state.collectLatest { st ->
                val cur = _uiState.value ?: HomeUiState()
                _uiState.postValue(cur.copy(isLoading = false, currentStreakDays = st.currentStreak, errorMessage = null))
            }
        }

        // 3. Load History & Calculate "Today" / "Week" in Memory
        dataJob?.cancel()
        dataJob = viewModelScope.launch(Dispatchers.IO) {
            history.observeRecent(limit = 500).collectLatest { allSessions ->
                val now = LocalDate.now()
                val zone = ZoneId.systemDefault()

                val todaySessions = allSessions.filter {
                    val sessionDate = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
                    sessionDate.isEqual(now)
                }
                val todayMinutes = todaySessions.sumOf { ((it.durationSeconds + 59) / 60).coerceAtLeast(0) }
                val todayCount = todaySessions.size

                val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val nextMonday = monday.plusDays(7)

                val weekSessionsList = allSessions.filter {
                    val sessionDate = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
                    !sessionDate.isBefore(monday) && sessionDate.isBefore(nextMonday)
                }
                val weekMinutes = weekSessionsList.sumOf { ((it.durationSeconds + 59) / 60).coerceAtLeast(0) }
                val weekCount = weekSessionsList.size

                val recentDisplay = allSessions.take(3).map { s ->
                    RecentActivity(
                        title = s.activityName,
                        timeAgo = timeAgoLabel(s.timestamp),
                        duration = "${((s.durationSeconds + 59) / 60).coerceAtLeast(1)} min",
                        category = s.category
                    )
                }

                val cur = _uiState.value ?: HomeUiState()
                _uiState.postValue(
                    cur.copy(
                        isLoading = false,
                        todayMinutes = todayMinutes,
                        todaySessionsCount = todayCount,
                        weekMinutes = weekMinutes,
                        weekSessions = weekCount,
                        recentActivities = recentDisplay,
                        errorMessage = null
                    )
                )
            }
        }
    }

    private fun timeAgoLabel(endedAtMillis: Long): String {
        if (endedAtMillis <= 0L) return ""
        val now = System.currentTimeMillis()
        val delta = (now - endedAtMillis).coerceAtLeast(0L)
        val minutes = delta / 60_000L
        if (minutes < 1) return "Just now"
        if (minutes < 60) return "${minutes} min ago"
        val hours = minutes / 60
        if (hours < 24) return "${hours} hours ago"

        val endedDate = Instant.ofEpochMilli(endedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val days = java.time.temporal.ChronoUnit.DAYS.between(endedDate, LocalDate.now()).toInt().coerceAtLeast(1)
        return if (days == 1) "Yesterday" else "${days} days ago"
    }

    override fun onCleared() {
        super.onCleared()
        settingsReg?.remove()
        dataJob?.cancel()
        streakJob?.cancel()
    }
}