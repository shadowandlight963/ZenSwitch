package com.example.ourmajor.data.progress

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.ourmajor.common.Result

class ProgressRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun recordSession(
        title: String,
        category: String,
        minutes: Int,
        startedAtMillis: Long,
        endedAtMillis: Long,
        completed: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(Result.Failure(IllegalStateException("User is not authenticated")))
            return
        }

        val endedDate = Instant.ofEpochMilli(endedAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val dayId = endedDate.toDayId()

        val userRef = firestore.collection("users").document(uid)
        val historyRef = userRef.collection("history").document()
        val dayRef = userRef.collection("dailyStats").document(dayId)

        firestore.runTransaction { tx ->
            val sessionId = historyRef.id

            val sessionData = hashMapOf(
                "id" to sessionId,
                "title" to title,
                "category" to category,
                "minutes" to minutes,
                "completed" to completed,
                "startedAt" to Timestamp(Instant.ofEpochMilli(startedAtMillis).epochSecond, 0),
                "endedAt" to Timestamp(Instant.ofEpochMilli(endedAtMillis).epochSecond, 0),
                "dayId" to dayId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            tx.set(historyRef, sessionData)

            if (!completed) {
                return@runTransaction null
            }

            val pointsDelta = (minutes * 10).coerceAtLeast(0)

            tx.set(
                dayRef,
                hashMapOf(
                    "dayId" to dayId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastSessionAt" to Timestamp(Instant.ofEpochMilli(endedAtMillis).epochSecond, 0)
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            tx.update(dayRef, "totalMinutes", FieldValue.increment(minutes.toLong()))
            tx.update(dayRef, "totalSessions", FieldValue.increment(1))
            tx.update(dayRef, "completedSessions", FieldValue.increment(1))

            val userSnap = tx.get(userRef)
            val lastActiveDayId = userSnap.getString("lastActiveDayId")
            val currentStreak = (userSnap.getLong("currentStreak") ?: 0L).toInt()
            val bestStreak = (userSnap.getLong("bestStreak") ?: 0L).toInt()

            val newStreak = computeNewStreak(lastActiveDayId, dayId, currentStreak)
            val newBest = maxOf(bestStreak, newStreak)

            tx.set(
                userRef,
                hashMapOf(
                    "lastActiveDayId" to dayId,
                    "currentStreak" to newStreak,
                    "bestStreak" to newBest,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            tx.update(userRef, "totalMinutes", FieldValue.increment(minutes.toLong()))
            tx.update(userRef, "totalSessions", FieldValue.increment(1))
            tx.update(userRef, "points", FieldValue.increment(pointsDelta.toLong()))

            null
        }
            .addOnSuccessListener { onResult(Result.Success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.Failure(e)) }
    }

    private fun computeNewStreak(lastActiveDayId: String?, newDayId: String, currentStreak: Int): Int {
        if (lastActiveDayId.isNullOrBlank()) return 1
        if (lastActiveDayId == newDayId) return currentStreak.coerceAtLeast(1)

        val last = runCatching { LocalDate.parse(lastActiveDayId) }.getOrNull() ?: return 1
        val next = last.plusDays(1)
        val newDay = runCatching { LocalDate.parse(newDayId) }.getOrNull() ?: return 1

        return if (newDay == next) currentStreak + 1 else 1
    }

    fun listenUserSummary(onResult: (Result<UserSummary>) -> Unit): ListenerRegistration? {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.Failure(IllegalStateException("User is not authenticated")))
            return null
        }

        val ref = firestore.collection("users").document(user.uid)
        return ref.addSnapshotListener { snap, err ->
            if (err != null) {
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }
            val s = snap
            if (s == null || !s.exists()) {
                onResult(
                    Result.Success(
                        UserSummary(
                            uid = user.uid,
                            email = user.email,
                            points = 0,
                            totalMinutes = 0,
                            totalSessions = 0,
                            currentStreak = 0,
                            bestStreak = 0,
                            lastActiveDayId = null
                        )
                    )
                )
                return@addSnapshotListener
            }

            onResult(
                Result.Success(
                    UserSummary(
                        uid = user.uid,
                        email = user.email,
                        points = (s.getLong("points") ?: 0L).toInt(),
                        totalMinutes = (s.getLong("totalMinutes") ?: 0L).toInt(),
                        totalSessions = (s.getLong("totalSessions") ?: 0L).toInt(),
                        currentStreak = (s.getLong("currentStreak") ?: 0L).toInt(),
                        bestStreak = (s.getLong("bestStreak") ?: 0L).toInt(),
                        lastActiveDayId = s.getString("lastActiveDayId")
                    )
                )
            )
        }
    }

    fun listenRecentSessions(limit: Long, onResult: (Result<List<ActivitySession>>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(Result.Failure(IllegalStateException("User is not authenticated")))
            return null
        }

        val ref = firestore.collection("users")
            .document(uid)
            .collection("history")
            .orderBy("endedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)

        return ref.addSnapshotListener { snap, err ->
            if (err != null) {
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }
            val docs = snap?.documents.orEmpty()
            val list = docs.mapNotNull { d ->
                val title = d.getString("title") ?: return@mapNotNull null
                val category = d.getString("category") ?: ""
                val minutes = (d.getLong("minutes") ?: 0L).toInt()
                val completed = d.getBoolean("completed") ?: false
                val startedAt = (d.getTimestamp("startedAt")?.toDate()?.time) ?: 0L
                val endedAt = (d.getTimestamp("endedAt")?.toDate()?.time) ?: 0L
                val dayId = d.getString("dayId") ?: ""
                ActivitySession(
                    id = d.id,
                    title = title,
                    category = category,
                    minutes = minutes,
                    completed = completed,
                    startedAtMillis = startedAt,
                    endedAtMillis = endedAt,
                    dayId = dayId
                )
            }
            onResult(Result.Success(list))
        }
    }

    fun listenDailyStats(dayIds: List<String>, onResult: (Result<Map<String, DailyStats>>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(Result.Failure(IllegalStateException("User is not authenticated")))
            return null
        }

        if (dayIds.isEmpty()) {
            onResult(Result.Success(emptyMap()))
            return null
        }

        val ref = firestore.collection("users")
            .document(uid)
            .collection("dailyStats")
            .whereIn("dayId", dayIds)

        return ref.addSnapshotListener { snap, err ->
            if (err != null) {
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }

            val map = snap?.documents.orEmpty().mapNotNull { d ->
                val dayId = d.getString("dayId") ?: d.id
                val totalMinutes = (d.getLong("totalMinutes") ?: 0L).toInt()
                val totalSessions = (d.getLong("totalSessions") ?: 0L).toInt()
                val completedSessions = (d.getLong("completedSessions") ?: 0L).toInt()
                val lastSessionAt = d.getTimestamp("lastSessionAt")?.toDate()?.time
                dayId to DailyStats(
                    dayId = dayId,
                    totalMinutes = totalMinutes,
                    totalSessions = totalSessions,
                    completedSessions = completedSessions,
                    lastSessionAtMillis = lastSessionAt
                )
            }.toMap()

            onResult(Result.Success(map))
        }
    }

    fun listenDailyStatsRange(
        startDayId: String,
        endDayId: String,
        onResult: (Result<Map<String, DailyStats>>) -> Unit
    ): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(Result.Failure(IllegalStateException("User is not authenticated")))
            return null
        }

        val ref = firestore.collection("users")
            .document(uid)
            .collection("dailyStats")
            .whereGreaterThanOrEqualTo("dayId", startDayId)
            .whereLessThanOrEqualTo("dayId", endDayId)

        return ref.addSnapshotListener { snap, err ->
            if (err != null) {
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }

            val map = snap?.documents.orEmpty().mapNotNull { d ->
                val dayId = d.getString("dayId") ?: d.id
                val totalMinutes = (d.getLong("totalMinutes") ?: 0L).toInt()
                val totalSessions = (d.getLong("totalSessions") ?: 0L).toInt()
                val completedSessions = (d.getLong("completedSessions") ?: 0L).toInt()
                val lastSessionAt = d.getTimestamp("lastSessionAt")?.toDate()?.time
                dayId to DailyStats(
                    dayId = dayId,
                    totalMinutes = totalMinutes,
                    totalSessions = totalSessions,
                    completedSessions = completedSessions,
                    lastSessionAtMillis = lastSessionAt
                )
            }.toMap()

            onResult(Result.Success(map))
        }
    }

    fun listenDay(dayId: String, onResult: (Result<DailyStats>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(Result.Failure(IllegalStateException("User is not authenticated")))
            return null
        }

        val ref = firestore.collection("users")
            .document(uid)
            .collection("dailyStats")
            .document(dayId)

        return ref.addSnapshotListener { snap, err ->
            if (err != null) {
                onResult(Result.Failure(err))
                return@addSnapshotListener
            }

            val s = snap
            if (s == null || !s.exists()) {
                onResult(Result.Success(DailyStats(dayId, 0, 0, 0, null)))
                return@addSnapshotListener
            }

            val totalMinutes = (s.getLong("totalMinutes") ?: 0L).toInt()
            val totalSessions = (s.getLong("totalSessions") ?: 0L).toInt()
            val completedSessions = (s.getLong("completedSessions") ?: 0L).toInt()
            val lastSessionAt = s.getTimestamp("lastSessionAt")?.toDate()?.time

            onResult(
                Result.Success(
                    DailyStats(
                        dayId = dayId,
                        totalMinutes = totalMinutes,
                        totalSessions = totalSessions,
                        completedSessions = completedSessions,
                        lastSessionAtMillis = lastSessionAt
                    )
                )
            )
        }
    }
}
