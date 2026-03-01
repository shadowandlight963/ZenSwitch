package com.example.ourmajor.data.profile

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val bio: String = "",
    val age: Int? = null,
    val gender: String = "",
    val mobileNumber: String = "",
    val mindfulnessLevel: String = "",
    val primaryGoal: String = "",
    val photoUrl: String? = null,
    val createdAt: Long? = null,
    val lastLoginAt: Long? = null
)

data class UserPreferences(
    val dailyGoalMinutes: Int = 30,
    val theme: String = "sage",
    val pushNotificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 9,
    val dailyReminderMinute: Int = 0
)

data class UserDocument(
    val profile: UserProfile = UserProfile(),
    val preferences: UserPreferences = UserPreferences()
)
