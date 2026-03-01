package com.example.ourmajor.ui.mindfulness

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.ourmajor.R

/** Placeholder for a future mindfulness activity. Replace with real implementation. */
class MindfulnessPlaceholder2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: MindfulnessPlaceholder2Activity")
        setContentView(R.layout.activity_mindfulness_session) // Reuse existing layout
    }
    
    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: MindfulnessPlaceholder2Activity")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: MindfulnessPlaceholder2Activity")
    }
}
