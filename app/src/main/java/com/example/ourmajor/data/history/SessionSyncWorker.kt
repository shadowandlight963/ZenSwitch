package com.example.ourmajor.data.history

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class SessionSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.success()
        }

        val dao = AppDatabase.get(applicationContext).sessionDao()
        val firestore = FirebaseFirestore.getInstance()

        return try {
            withContext(Dispatchers.IO) {
                val dirty = dao.getDirty(limit = 50)
                for (s in dirty) {
                    syncOne(firestore, uid, dao, s)
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncOne(
        firestore: FirebaseFirestore,
        uid: String,
        dao: SessionDao,
        entity: SessionEntity
    ) {
        val doc = firestore
            .collection("users")
            .document(uid)
            .collection("sessions")
            .document(entity.id)

        val payload = hashMapOf(
            "id" to entity.id,
            "activityName" to entity.activityName,
            "category" to entity.category,
            "durationSeconds" to entity.durationSeconds,
            "timestamp" to entity.timestamp,
            "pointsEarned" to entity.pointsEarned,
            "updatedAtMillis" to entity.updatedAtMillis,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )

        doc.set(payload, SetOptions.merge()).await()

        val snap = doc.get().await()
        val remoteUpdatedAtMillis = snap.getTimestamp("serverUpdatedAt")?.toDate()?.time ?: System.currentTimeMillis()

        dao.markSynced(
            id = entity.id,
            syncedAtMillis = System.currentTimeMillis(),
            remoteUpdatedAtMillis = remoteUpdatedAtMillis
        )
    }

    companion object {
        const val TAG: String = "session_sync"
        const val UNIQUE_WORK_NAME: String = "session_sync_once"
    }
}
