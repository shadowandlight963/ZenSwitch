package com.example.ourmajor.ui.journaling

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.ourmajor.R

/** Placeholder for a future journaling activity. Replace with real implementation. */
class JournalingPlaceholder1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: JournalingPlaceholder1Activity")
        setContentView(R.layout.activity_journaling_session) // Reuse existing layout
    }
    
    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: JournalingPlaceholder1Activity")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: JournalingPlaceholder1Activity")
    }
}
