package com.example.ourmajor.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ourmajor.common.BaseViewModel
import com.example.ourmajor.common.Result
import com.example.ourmajor.common.onSuccess
import com.example.ourmajor.common.onFailure
import com.example.ourmajor.data.profile.ProfileRepository
import com.example.ourmajor.data.profile.UserSettings
import com.example.ourmajor.data.profile.UserPreferences
import com.google.firebase.firestore.ListenerRegistration

class ProfileViewModel(
    private val repo: ProfileRepository = ProfileRepository()
) : BaseViewModel() {

    private val _settings = MutableLiveData<UserPreferences>()
    val settings: LiveData<UserPreferences> = _settings

    private var settingsReg: ListenerRegistration? = null

    init {
        start()
    }

    private fun start() {
        execute<Unit>(tag = "ProfileViewModel", loading = true, block = {
            repo.ensureUserDocument()
        })

        settingsReg = repo.listenUserDocument { result ->
            result
                .onSuccess { doc ->
                    _settings.postValue(doc.preferences)
                    _isLoading.postValue(false)
                    _errorMessage.postValue(null)
                }
                .onFailure { e ->
                    _isLoading.postValue(false)
                    _errorMessage.postValue(e.message ?: "Failed to load settings")
                }
        }
    }

    fun updateDailyGoalMinutes(minutes: Int) {
        val current = _settings.value ?: return
        val updated = current.copy(dailyGoalMinutes = minutes.coerceIn(1, 180))
        save(updated)
    }

    fun setTheme(theme: String) {
        val current = _settings.value ?: return
        val updated = current.copy(theme = theme)
        save(updated)
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        val current = _settings.value ?: return
        val updated = current.copy(pushNotificationsEnabled = enabled)
        save(updated)
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        val current = _settings.value ?: return
        val updated = current.copy(dailyReminderEnabled = enabled)
        save(updated)
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        val current = _settings.value ?: return
        val updated = current.copy(dailyReminderHour = hour, dailyReminderMinute = minute)
        save(updated)
    }

    private fun save(settings: UserPreferences) {
        execute<Unit>(tag = "ProfileViewModel", block = {
            repo.updatePreferences(settings)
        })
    }

    override fun onCleared() {
        super.onCleared()
        settingsReg?.remove()
    }
}
