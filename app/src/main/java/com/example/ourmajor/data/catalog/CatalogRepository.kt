package com.example.ourmajor.data.catalog

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import com.example.ourmajor.common.Result

class CatalogRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val categoriesCollection = firestore.collection("catalog").document("static").collection("categories")
    private val activitiesCollection = firestore.collection("catalog").document("static").collection("activities")

    fun listenCategories(onResult: (Result<List<ActivityCategory>>) -> Unit): ListenerRegistration? {
        return categoriesCollection.addSnapshotListener { snap, err ->
            if (err != null) {
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }

            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(ActivityCategory::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            onResult(Result.Success(list))
        }
    }

    fun listenActivities(onResult: (Result<List<Activity>>) -> Unit): ListenerRegistration? {
        return activitiesCollection.addSnapshotListener { snap, err ->
            if (err != null) {
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }

            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(Activity::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            onResult(Result.Success(list))
        }
    }

    suspend fun ensureSeeded() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) return

        // Only seed if categories collection is empty
        val categoriesSnap = categoriesCollection.limit(1).get().await()
        if (!categoriesSnap.isEmpty) return

        // Seed categories
        val categories = listOf(
            ActivityCategory("meditation", "Meditation", "ic_meditation", "color_meditation", "Calm your mind"),
            ActivityCategory("breathing", "Breathing", "ic_breathing", "color_breathing", "Refresh your body"),
            ActivityCategory("yoga", "Yoga", "ic_yoga", "color_yoga", "Stretch and strengthen"),
            ActivityCategory("focus", "Focus", "ic_focus", "color_focus", "Sharpen concentration")
        )
        categories.forEach { cat ->
            categoriesCollection.document(cat.id).set(cat)
        }

        // Seed activities
        val activities = listOf(
            // Meditation
            Activity("med_1", "meditation", "Body Scan", "Full body awareness scan", 10, "ic_meditation"),
            Activity("med_2", "meditation", "Mindful Breathing", "Focus on breath", 5, "ic_meditation"),
            Activity("med_3", "meditation", "Loving Kindness", "Compassion meditation", 15, "ic_meditation"),
            // Breathing
            Activity("br_1", "breathing", "4-7-8 Breath", "Relaxation breathing", 5, "ic_breathing"),
            Activity("br_2", "breathing", "Box Breathing", "Focus and calm", 7, "ic_breathing"),
            Activity("br_3", "breathing", "Diaphragmatic", "Deep belly breathing", 8, "ic_breathing"),
            // Yoga
            Activity("yoga_1", "yoga", "Sun Salutation", "Energizing flow", 12, "ic_yoga"),
            Activity("yoga_2", "yoga", "Child's Pose", "Restorative stretch", 6, "ic_yoga"),
            Activity("yoga_3", "yoga", "Cat-Cow", "Spinal flexibility", 8, "ic_yoga"),
            // Focus
            Activity("focus_1", "focus", "Pomodoro Sprint", "25-minute focus", 25, "ic_focus"),
            Activity("focus_2", "focus", "Mindful Work", "Work awareness", 15, "ic_focus"),
            Activity("focus_3", "focus", "Digital Detox", "Screen-free time", 20, "ic_focus")
        )
        activities.forEach { act ->
            activitiesCollection.document(act.id).set(act)
        }
    }
}
