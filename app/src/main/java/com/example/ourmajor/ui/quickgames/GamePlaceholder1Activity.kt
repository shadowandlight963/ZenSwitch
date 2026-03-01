package com.example.ourmajor.ui.quickgames

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.ourmajor.R

/** Placeholder for a future quick game activity. Replace with real implementation. */
class GamePlaceholder1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: GamePlaceholder1Activity")
        setContentView(R.layout.activity_quick_games) // Reuse existing layout
    }
    
    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: GamePlaceholder1Activity")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: GamePlaceholder1Activity")
    }
}
