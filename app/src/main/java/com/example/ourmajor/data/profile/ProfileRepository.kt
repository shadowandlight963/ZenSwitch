package com.example.ourmajor.data.profile

import android.util.Log
import androidx.core.net.toUri
import com.example.ourmajor.common.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val tag = "ProfileRepository"

    suspend fun ensureUserDocument(): Result<Unit> {
        val user = auth.currentUser ?: return Result.Failure(IllegalStateException("User not authenticated"))
        val docRef = firestore.collection("users").document(user.uid)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val now = System.currentTimeMillis()
                val userDoc = UserDocument(
                    profile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "",
                        bio = "",
                        age = null,
                        gender = "",
                        mobileNumber = "",
                        mindfulnessLevel = "",
                        primaryGoal = "",
                        photoUrl = user.photoUrl?.toString(),
                        createdAt = now,
                        lastLoginAt = now
                    )
                )
                docRef.set(userDoc).await()
                Log.d(tag, "Created user document for ${user.uid}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to ensure user document", e)
            Result.Failure(e)
        }
    }

    fun listenUserDocument(onResult: (Result<UserDocument>) -> Unit): ListenerRegistration? {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.Failure(IllegalStateException("User is not authenticated")))
            return null
        }

        val ref = firestore.collection("users").document(user.uid)
        return ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(tag, "Failed to listen user document", err)
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }

            if (snap == null || !snap.exists()) {
                onResult(Result.Failure(IllegalStateException("User document does not exist")))
                return@addSnapshotListener
            }

            try {
                val doc = snap.toObject(UserDocument::class.java)
                if (doc != null) {
                    onResult(Result.Success(doc))
                } else {
                    onResult(Result.Failure(IllegalStateException("Failed to parse user document")))
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse user document", e)
                onResult(Result.Failure(e))
            }
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.Failure(IllegalStateException("User is not authenticated"))
        }

        return try {
            val user = auth.currentUser
            if (user != null) {
                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(profile.displayName)
                    .setPhotoUri(profile.photoUrl?.takeIf { it.isNotBlank() }?.toUri())
                    .build()
                user.updateProfile(req).await()
            }

            val ref = firestore.collection("users").document(uid)
            ref.update("profile", profile).await()
            Log.d(tag, "Updated profile for $uid")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update profile", e)
            Result.Failure(e)
        }
    }

    suspend fun updatePreferences(preferences: UserPreferences): Result<Unit> {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.Failure(IllegalStateException("User is not authenticated"))
        }

        return try {
            val ref = firestore.collection("users").document(uid)
            ref.update("preferences", preferences).await()
            Log.d(tag, "Updated preferences for $uid")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update preferences", e)
            Result.Failure(e)
        }
    }
}
