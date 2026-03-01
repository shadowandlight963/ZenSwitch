package com.example.ourmajor.data.history

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SessionRepository(
    context: Context,
    private val dao: SessionDao = AppDatabase.get(context).sessionDao(),
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val appContext = context.applicationContext

    suspend fun saveSession(session: Session) {
        val now = System.currentTimeMillis()

        dao.upsert(
            SessionEntity(
                id = session.id,
                activityName = session.activityName,
                category = session.category,
                durationSeconds = session.durationSeconds,
                timestamp = session.timestamp,
                pointsEarned = session.pointsEarned,
                isDirty = true,
                updatedAtMillis = now
            )
        )

        enqueueSync()
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = OneTimeWorkRequestBuilder<SessionSyncWorker>()
            .setConstraints(constraints)
            .addTag(SessionSyncWorker.TAG)
            .build()

        workManager.enqueueUniqueWork(SessionSyncWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, req)
    }
}
