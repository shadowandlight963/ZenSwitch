package com.example.ourmajor.data.profile

data class UserSettings(
    val dailyGoalMinutes: Int = 30,
    val theme: String = "sage",
    val pushNotificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 9,
    val dailyReminderMinute: Int = 0
)
