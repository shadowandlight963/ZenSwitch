package com.example.ourmajor.data.mindfulness

import android.content.Context

class GratitudeRepository(context: Context) {

    private val dao = MindfulnessDatabase.get(context).gratitudeDao()

    suspend fun addEntry(text: String): Long {
        return dao.insert(
            GratitudeEntry(
                text = text,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun getAll(): List<GratitudeEntry> = dao.getAll()
}
