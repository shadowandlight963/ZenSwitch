package com.example.ourmajor.data.notifications

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import com.example.ourmajor.common.Result
import com.example.ourmajor.data.profile.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class ReminderRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val tag = "ReminderRepository"
    private val workManager = WorkManager.getInstance(context)

    suspend fun scheduleReminder(preferences: UserPreferences): Result<Unit> {
        return try {
            // Cancel existing
            workManager.cancelAllWorkByTag("daily_reminder").await()

            if (preferences.dailyReminderEnabled) {
                // Calculate initial delay to next reminder time
                val now = System.currentTimeMillis()
                val targetHour = preferences.dailyReminderHour
                val targetMinute = preferences.dailyReminderMinute

                val targetTime = java.time.LocalDate.now()
                    .atTime(targetHour, targetMinute)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val initialDelay = if (targetTime > now) {
                    targetTime - now
                } else {
                    // If time passed today, schedule for tomorrow
                    TimeUnit.DAYS.toMillis(1) - (now - targetTime)
                }

                val workRequest = PeriodicWorkRequest.Builder(
                    ReminderWorker::class.java,
                    1, TimeUnit.DAYS
                )
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag("daily_reminder")
                    .setInputData(
                        Data.Builder()
                            .putString(ReminderWorker.KEY_TITLE, "ZenSwitch Reminder")
                            .putString(ReminderWorker.KEY_MESSAGE, "Time for a mindful moment!")
                            .build()
                    )
                    .build()

                workManager.enqueue(workRequest).await()
                Log.d(tag, "Scheduled daily reminder at $targetHour:$targetMinute")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to schedule reminder", e)
            Result.Failure(e)
        }
    }

    suspend fun cancelReminder(): Result<Unit> {
        return try {
            workManager.cancelAllWorkByTag("daily_reminder").await()
            Log.d(tag, "Cancelled daily reminder")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to cancel reminder", e)
            Result.Failure(e)
        }
    }

    // Optional: Sync reminder schedule to Firestore
    suspend fun syncReminderToFirestore(preferences: UserPreferences): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.Failure(IllegalStateException("User not authenticated"))
        }

        return try {
            val ref = firestore.collection("users").document(uid)
            val data = mapOf(
                "preferences.dailyReminderEnabled" to preferences.dailyReminderEnabled,
                "preferences.dailyReminderHour" to preferences.dailyReminderHour,
                "preferences.dailyReminderMinute" to preferences.dailyReminderMinute,
                "reminderUpdatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            ref.update(data).await()
            Log.d(tag, "Synced reminder to Firestore")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync reminder to Firestore", e)
            Result.Failure(e)
        }
    }
}
