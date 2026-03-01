package com.example.ourmajor.ui.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.ourmajor.common.BaseViewModel
import com.example.ourmajor.common.Result
import com.example.ourmajor.common.onSuccess
import com.example.ourmajor.common.onFailure
import com.example.ourmajor.data.history.SessionHistoryRepository
import com.example.ourmajor.data.profile.ProfileRepository
import com.example.ourmajor.data.notifications.ReminderRepository
import com.example.ourmajor.data.profile.UserPreferences
import com.example.ourmajor.data.profile.UserProfile
import com.example.ourmajor.gamification.GamificationManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val Context.profilePrefsDataStore by preferencesDataStore(name = "profile_prefs")

data class ProfileUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val profile: UserProfile = UserProfile(),
    val preferences: UserPreferences = UserPreferences()
)

data class ProfileStats(
    val currentStreakDays: Int = 0,
    val totalPoints: Int = 0,
    val totalActivities: Int = 0,
    val totalMinutes: Int = 0,
    val currentLevel: Int = 0
)

class ProfileViewModel2(
    private val repo: ProfileRepository = ProfileRepository(),
    private val reminderRepo: ReminderRepository? = null,
    private val history: SessionHistoryRepository,
    private val gm: GamificationManager,
    private val appContext: Context? = null
) : BaseViewModel() {

    private val _uiState = MutableLiveData(ProfileUiState())
    val uiState: LiveData<ProfileUiState> = _uiState

    private val _stats = MutableLiveData(ProfileStats())
    val stats: LiveData<ProfileStats> = _stats

    private val _profileCompleteness = MutableLiveData(0)
    val profileCompleteness: LiveData<Int> = _profileCompleteness

    private var userDocReg: ListenerRegistration? = null

    private var streakJob: Job? = null
    private var totalActivitiesJob: Job? = null

    private val _selectedFocus = MutableStateFlow("")
    val selectedFocus: StateFlow<String> = _selectedFocus

    @Volatile
    private var focusPrefsLoaded: Boolean = false

    init {
        start()
        startStats()
        startFocus()
    }

    private fun canonicalizeFocus(value: String): String {
        val v = value.trim().lowercase()
        return when {
            v.contains("sleep") -> "Sleep"
            v.contains("energy") -> "Energy"
            v.contains("anxiety") -> "Fun"
            v.contains("fun") -> "Fun"
            v.contains("focus") -> "Focus"
            else -> ""
        }
    }

    private fun startFocus() {
        val ctx = appContext
        if (ctx == null) {
            focusPrefsLoaded = true
            return
        }
        viewModelScope.launch {
            val saved = ctx.profilePrefsDataStore.data.first()[KEY_SELECTED_FOCUS].orEmpty()
            _selectedFocus.value = canonicalizeFocus(saved)
            focusPrefsLoaded = true
        }
    }

    fun selectFocus(goal: String) {
        val normalized = canonicalizeFocus(goal)
        if (normalized.isBlank()) return
        if (_selectedFocus.value.equals(normalized, ignoreCase = true)) return

        _selectedFocus.value = normalized

        val ctx = appContext
        if (ctx != null) {
            viewModelScope.launch {
                ctx.profilePrefsDataStore.edit { p ->
                    p[KEY_SELECTED_FOCUS] = normalized
                }
            }
        }

        updatePrimaryGoal(normalized)
    }

    private fun startStats() {
        streakJob?.cancel()
        streakJob = viewModelScope.launch {
            gm.state.collectLatest { st ->
                val cur = _stats.value ?: ProfileStats()
                _stats.postValue(
                    cur.copy(
                        currentStreakDays = st.currentStreak,
                        totalPoints = st.totalPoints,
                        currentLevel = st.currentLevel
                    )
                )
            }
        }

        totalActivitiesJob?.cancel()
        totalActivitiesJob = viewModelScope.launch {
            history.observeTotalCount().collectLatest { count ->
                val cur = _stats.value ?: ProfileStats()
                _stats.postValue(cur.copy(totalActivities = count.coerceAtLeast(0)))
            }
        }

        viewModelScope.launch {
            history.observeTotalDurationSeconds().collectLatest { totalSeconds ->
                val cur = _stats.value ?: ProfileStats()
                val minutes = ((totalSeconds + 59) / 60).toInt().coerceAtLeast(0)
                _stats.postValue(cur.copy(totalMinutes = minutes))
            }
        }
    }

    private fun start() {
        execute<Unit>(tag = "ProfileViewModel", loading = true, block = {
            repo.ensureUserDocument()
        })

        userDocReg = repo.listenUserDocument { result ->
            result
                .onSuccess { doc ->
                    _uiState.postValue(_uiState.value?.copy(
                        isLoading = false,
                        profile = doc.profile,
                        preferences = doc.preferences,
                        errorMessage = null
                    ))

                    val existing = _selectedFocus.value
                    if (focusPrefsLoaded && existing.isBlank()) {
                        val fromProfile = canonicalizeFocus(doc.profile.primaryGoal)
                        if (fromProfile.isNotBlank()) {
                            _selectedFocus.value = fromProfile
                            val ctx = appContext
                            if (ctx != null) {
                                viewModelScope.launch {
                                    ctx.profilePrefsDataStore.edit { p ->
                                        p[KEY_SELECTED_FOCUS] = fromProfile
                                    }
                                }
                            }
                        }
                    }

                    _profileCompleteness.postValue(computeCompletenessPercent(doc.profile))
                }
                .onFailure { e ->
                    _uiState.postValue(_uiState.value?.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load profile"
                    ))
                }
        }
    }

    private fun computeCompletenessPercent(profile: UserProfile): Int {
        val total = 4
        var filled = 0
        if (profile.displayName.isNotBlank()) filled++
        if (profile.bio.isNotBlank()) filled++
        if (profile.mobileNumber.isNotBlank()) filled++
        if ((profile.age ?: 0) > 0) filled++
        return ((filled.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun updateDisplayName(name: String) {
        val current = _uiState.value ?: return
        if (name.isBlank()) {
            _errorMessage.postValue("Display name cannot be empty")
            return
        }
        val updated = current.profile.copy(displayName = name.trim())
        execute<Unit>(tag = "ProfileViewModel", block = { repo.updateProfile(updated) })
        // Optimistic update
        _uiState.postValue(current.copy(profile = updated))
        _profileCompleteness.postValue(computeCompletenessPercent(updated))
    }

    fun saveProfile(profile: UserProfile) {
        val current = _uiState.value ?: return
        execute<Unit>(tag = "ProfileViewModel", block = { repo.updateProfile(profile) })
        _uiState.postValue(current.copy(profile = profile))
        _profileCompleteness.postValue(computeCompletenessPercent(profile))
    }

    fun updatePrimaryGoal(goal: String) {
        val current = _uiState.value ?: return
        val updated = current.profile.copy(primaryGoal = goal.trim())
        execute<Unit>(tag = "ProfileViewModel", loading = false, block = { repo.updateProfile(updated) })
        _uiState.postValue(current.copy(profile = updated))
    }

    fun updateTheme(theme: String) {
        val current = _uiState.value ?: return
        val updated = current.preferences.copy(theme = theme)
        execute<Unit>(tag = "ProfileViewModel", block = {
            repo.updatePreferences(updated)
        })
        // Optimistic update
        _uiState.postValue(current.copy(preferences = updated))
    }

    fun updateDailyGoalMinutes(minutes: Int) {
        val current = _uiState.value ?: return
        val updated = current.preferences.copy(dailyGoalMinutes = minutes.coerceIn(1, 180))
        execute<Unit>(tag = "ProfileViewModel", block = {
            repo.updatePreferences(updated)
        })
        // Optimistic update
        _uiState.postValue(current.copy(preferences = updated))
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        val current = _uiState.value ?: return
        val updated = current.preferences.copy(pushNotificationsEnabled = enabled)
        execute<Unit>(tag = "ProfileViewModel", loading = false, block = {
            repo.updatePreferences(updated)
        })
        // Optimistic update
        _uiState.postValue(current.copy(preferences = updated))
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        val current = _uiState.value ?: return
        val updated = current.preferences.copy(dailyReminderEnabled = enabled)
        execute<Unit>(tag = "ProfileViewModel", loading = false, block = {
            repo.updatePreferences(updated)
            val reminderResult = reminderRepo?.scheduleReminder(updated)
            if (reminderResult == null) {
                // Log warning but don't fail the operation
                android.util.Log.w("ProfileViewModel", "ReminderRepository not available")
            }
            Result.Success(Unit)
        })
        // Optimistic update
        _uiState.postValue(current.copy(preferences = updated))
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        val current = _uiState.value ?: return
        val updated = current.preferences.copy(dailyReminderHour = hour, dailyReminderMinute = minute)
        execute<Unit>(tag = "ProfileViewModel", loading = false, block = {
            repo.updatePreferences(updated)
            val reminderResult = reminderRepo?.scheduleReminder(updated)
            if (reminderResult == null) {
                // Log warning but don't fail the operation
                android.util.Log.w("ProfileViewModel", "ReminderRepository not available")
            }
            Result.Success(Unit)
        })
        // Optimistic update
        _uiState.postValue(current.copy(preferences = updated))
    }

    override fun onCleared() {
        super.onCleared()
        userDocReg?.remove()
        streakJob?.cancel()
        totalActivitiesJob?.cancel()
    }

    companion object {
        private val KEY_SELECTED_FOCUS = stringPreferencesKey("selected_focus")
    }
}
