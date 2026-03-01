package com.example.ourmajor.ui.stretching

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.ourmajor.R

/** Placeholder for a future stretching activity. Replace with real implementation. */
class StretchPlaceholder1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: StretchPlaceholder1Activity")
        setContentView(R.layout.activity_stretching_session) // Reuse existing layout
    }
    
    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: StretchPlaceholder1Activity")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: StretchPlaceholder1Activity")
    }
}
