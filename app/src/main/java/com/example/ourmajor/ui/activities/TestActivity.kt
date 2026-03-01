package com.example.ourmajor.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ourmajor.R

/**
 * Simple test activity for notification tap handling.
 * Opens when user taps on a nudge notification.
 */
class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        
        setupUI()
    }

    private fun setupUI() {
        val titleText = findViewById<TextView>(R.id.test_title)
        val messageText = findViewById<TextView>(R.id.test_message)
        val closeButton = findViewById<Button>(R.id.btn_close_test)
        
        titleText.text = "Nudge Received!"
        messageText.text = "You tapped on a mindful nudge notification.\n\nThis screen confirms the notification system is working properly."
        
        closeButton.setOnClickListener {
            finish()
        }
    }
}
