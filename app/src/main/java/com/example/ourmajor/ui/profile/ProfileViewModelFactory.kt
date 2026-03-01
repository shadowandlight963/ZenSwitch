package com.example.ourmajor.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ourmajor.data.history.SessionHistoryRepository
import com.example.ourmajor.data.notifications.ReminderRepository
import com.example.ourmajor.gamification.GamificationManager

class ProfileViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel2::class.java)) {
            val appContext = context.applicationContext
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel2(
                reminderRepo = ReminderRepository(context),
                history = SessionHistoryRepository(appContext),
                gm = GamificationManager.getInstance(appContext),
                appContext = appContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
