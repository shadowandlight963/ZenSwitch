package com.example.ourmajor

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.example.ourmajor.notifications.NudgeNotificationHelper

class ZenSwitchApp : Application() {

    override fun onCreate() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val darkEnabled = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate()
        FirebaseApp.initializeApp(this)

        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        
        // Initialize notification channel for nudges
        NudgeNotificationHelper(this).createNotificationChannel()
    }
}
