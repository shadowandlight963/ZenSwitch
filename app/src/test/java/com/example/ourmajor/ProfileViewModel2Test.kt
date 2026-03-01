package com.example.ourmajor

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.ourmajor.common.Result
import com.example.ourmajor.data.profile.ProfileRepository
import com.example.ourmajor.data.profile.UserPreferences
import com.example.ourmajor.data.profile.UserProfile
import com.example.ourmajor.data.history.SessionHistoryRepository
import com.example.ourmajor.gamification.GamificationManager
import com.example.ourmajor.ui.profile.ProfileUiState
import com.example.ourmajor.ui.profile.ProfileViewModel2
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileViewModel2Test {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepo: ProfileRepository

    @Mock
    private lateinit var mockHistory: SessionHistoryRepository

    @Mock
    private lateinit var mockGm: GamificationManager

    private lateinit var viewModel: ProfileViewModel2

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = ProfileViewModel2(
            repo = mockRepo,
            reminderRepo = null,
            history = mockHistory,
            gm = mockGm
        )
    }

    @Test
    fun testUpdateDisplayName_BlankName_SetsError() {
        // Given
        val blankName = "   "

        // When
        viewModel.updateDisplayName(blankName)

        // Then
        val state = requireNotNull(viewModel.uiState.value)
        assertEquals("Display name cannot be empty", state.errorMessage)
    }

    @Test
    fun testUpdateTheme_UpdatesState() {
        // Given
        val theme = "ocean"

        // When
        viewModel.updateTheme(theme)

        // Then
        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(theme, state.preferences.theme)
    }

    @Test
    fun testUpdateDailyGoalMinutes_UpdatesState() {
        // Given
        val minutes = 45

        // When
        viewModel.updateDailyGoalMinutes(minutes)

        // Then
        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(minutes, state.preferences.dailyGoalMinutes)
    }

    @Test
    fun testUpdateDailyGoalMinutes_ClampsValue() {
        // Given
        val invalidMinutes = 200

        // When
        viewModel.updateDailyGoalMinutes(invalidMinutes)

        // Then
        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(180, state.preferences.dailyGoalMinutes) // Clamped to max
    }

    @Test
    fun testSetPushNotificationsEnabled_UpdatesState() {
        // Given
        val enabled = false

        // When
        viewModel.setPushNotificationsEnabled(enabled)

        // Then
        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(enabled, state.preferences.pushNotificationsEnabled)
    }

    @Test
    fun testSetDailyReminderEnabled_UpdatesState() {
        // Given
        val enabled = true

        // When
        viewModel.setDailyReminderEnabled(enabled)

        // Then
        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(enabled, state.preferences.dailyReminderEnabled)
    }

    @Test
    fun testSetDailyReminderTime_UpdatesState() {
        // Given
        val hour = 10
        val minute = 30

        // When
        viewModel.setDailyReminderTime(hour, minute)

        // Then
        val state = requireNotNull(viewModel.uiState.value)
        assertEquals(hour, state.preferences.dailyReminderHour)
        assertEquals(minute, state.preferences.dailyReminderMinute)
    }
}
