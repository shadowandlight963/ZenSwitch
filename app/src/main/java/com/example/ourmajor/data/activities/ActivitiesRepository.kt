package com.example.ourmajor.data.activities

import android.content.Context
import android.util.Log
import com.example.ourmajor.common.Result
import com.example.ourmajor.common.onSuccess
import com.example.ourmajor.common.onFailure
import com.example.ourmajor.data.activities.Favorite
import com.example.ourmajor.data.catalog.Activity
import com.example.ourmajor.data.catalog.CatalogRepository
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.data.progress.ActivitySession
import com.example.ourmajor.gamification.ActivityType
import com.example.ourmajor.gamification.GamificationManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ActivitiesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val catalogRepo: CatalogRepository = CatalogRepository(),
    private val progressRepo: ProgressRepository = ProgressRepository()
) {
    private val tag = "ActivitiesRepository"

    private val appContext: Context by lazy { FirebaseApp.getInstance().applicationContext }
    private val gm: GamificationManager by lazy { GamificationManager.getInstance(appContext) }

    private fun getFavoritesCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("favorites")
    }

    fun listenFavorites(onResult: (Result<List<Favorite>>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(Result.Failure(IllegalStateException("User not authenticated")))
            return null
        }

        return getFavoritesCollection()?.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(tag, "Failed to listen favorites", err)
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }

            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(Favorite::class.java)?.copy(activityId = doc.id)
            } ?: emptyList()
            onResult(Result.Success(list))
        }
    }

    suspend fun addFavorite(activityId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.Failure(IllegalStateException("User not authenticated"))
        }

        return try {
            getFavoritesCollection()?.document(activityId)?.set(Favorite(activityId))?.await()
            Log.d(tag, "Added favorite $activityId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add favorite", e)
            Result.Failure(e)
        }
    }

    suspend fun removeFavorite(activityId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.Failure(IllegalStateException("User not authenticated"))
        }

        return try {
            getFavoritesCollection()?.document(activityId)?.delete()?.await()
            Log.d(tag, "Removed favorite $activityId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to remove favorite", e)
            Result.Failure(e)
        }
    }

    // Progress tracking via existing ProgressRepository
    suspend fun recordSession(activity: Activity, minutes: Int, completed: Boolean): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            progressRepo.recordSession(
                title = activity.name,
                category = activity.categoryId,
                minutes = minutes,
                startedAtMillis = now - (minutes * 60 * 1000),
                endedAtMillis = now,
                completed = completed
            ) { result ->
                // Handle result synchronously
            }

            if (completed) {
                val resolvedType = when (activity.categoryId.lowercase()) {
                    "breathing" -> ActivityType.BREATHING
                    "stretching" -> ActivityType.STRETCHING
                    "journaling" -> ActivityType.JOURNALING
                    "games" -> ActivityType.GAMES
                    "mindfulness" -> ActivityType.MINDFULNESS
                    else -> ActivityType.MINDFULNESS
                }
                withContext(Dispatchers.IO) {
                    gm.awardPoints(
                        activityType = resolvedType,
                        duration = minutes,
                        completedAtMillis = now,
                        activityName = activity.name,
                        category = activity.categoryId
                    )
                }
            }

            Log.d(tag, "Recorded session for ${activity.name}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to record session", e)
            Result.Failure(e)
        }
    }

    fun listenRecentSessions(limit: Int = 10, onResult: (Result<List<ActivitySession>>) -> Unit): ListenerRegistration? {
        return progressRepo.listenRecentSessions(limit.toLong()) { result ->
            result
                .onSuccess { list -> onResult(Result.Success(list)) }
                .onFailure { e -> onResult(Result.Failure(e)) }
        }
    }

    fun listenActivityHistory(activityId: String, onResult: (Result<List<ActivitySession>>) -> Unit): ListenerRegistration? {
        // TODO: Implement filtered history by activityId if needed
        return listenRecentSessions(50) { result ->
            result
                .onSuccess { list ->
                    // Filter by activity name (since we don't store activityId in history)
                    val filtered = list.filter { it.title.contains(activityId, ignoreCase = true) }
                    onResult(Result.Success(filtered))
                }
                .onFailure { e -> onResult(Result.Failure(e)) }
        }
    }
}
