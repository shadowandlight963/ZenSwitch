package com.example.ourmajor.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.google.android.material.progressindicator.CircularProgressIndicator

class ExerciseActivity : AppCompatActivity() {
    private lateinit var titleView: TextView
    private lateinit var skipBtn: TextView
    private lateinit var timerText: TextView
    private lateinit var instructionText: TextView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var pulseCircle: View

    private var totalMillis: Long = 60_000
    private var isBreathing: Boolean = false
    private var category: String = ""
    private var timer: CountDownTimer? = null

    private val progressRepository: ProgressRepository by lazy { ProgressRepository() }

    private var sessionStartedAtMillis: Long = 0L
    private var sessionRecorded: Boolean = false
    private var sessionTitle: String = "Activity"
    private var sessionMinutes: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_exercise)

            titleView = findViewById(R.id.tvExerciseTitle)
            skipBtn = findViewById(R.id.btnSkip)
            timerText = findViewById(R.id.tvTimer)
            instructionText = findViewById(R.id.tvInstruction)
            progress = findViewById(R.id.timerRing)
            pulseCircle = findViewById(R.id.pulseCircle)
        } catch (e: Exception) {
            android.util.Log.e("ExerciseActivity", "Failed to initialize ExerciseActivity", e)
            finish()
            return
        }

        try {
            val name = intent.getStringExtra(EXTRA_NAME) ?: "Activity"
            val minutes = intent.getIntExtra(EXTRA_MINUTES, 1)
            isBreathing = intent.getBooleanExtra(EXTRA_BREATHING, false)
            category = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
            totalMillis = minutes * 60_000L

            sessionTitle = name
            sessionMinutes = minutes
            sessionStartedAtMillis = System.currentTimeMillis()

            titleView.text = name
            progress.max = (totalMillis / 1000).toInt()

            findViewById<ImageButton>(R.id.btnClose).setOnClickListener { recordAndFinish(false) }
            skipBtn.setOnClickListener { recordAndFinish(false) }

            if (isBreathing) {
                instructionText.visibility = View.VISIBLE
                pulseCircle.visibility = View.VISIBLE
            } else {
                pulseCircle.visibility = View.GONE
                instructionText.visibility = View.VISIBLE
            }

            // Start breathing pulse animation if applicable
            if (isBreathing) {
                val anim = AnimationUtils.loadAnimation(this, R.anim.breathe_pulse)
                pulseCircle.startAnimation(anim)
            }

            startTimer()
        } catch (e: Exception) {
            android.util.Log.e("ExerciseActivity", "Failed to setup exercise activity", e)
            finish()
        }
    }

    private fun recordAndFinish(completed: Boolean) {
        if (sessionRecorded) {
            finish()
            return
        }
        sessionRecorded = true

        val endedAt = System.currentTimeMillis()
        try {
            progressRepository.recordSession(
                title = sessionTitle,
                category = category,
                minutes = sessionMinutes,
                startedAtMillis = sessionStartedAtMillis,
                endedAtMillis = endedAt,
                completed = completed
            ) { result ->
                // Handle result but don't block UI
                when (result) {
                    is com.example.ourmajor.common.Result.Success -> {
                        // Session recorded successfully
                    }
                    is com.example.ourmajor.common.Result.Failure -> {
                        // Log error but don't crash
                        android.util.Log.e("ExerciseActivity", "Failed to record session", result.exception)
                    }
                }
            }
        } catch (e: Exception) {
            // Catch any exceptions to prevent crash
            android.util.Log.e("ExerciseActivity", "Exception during session recording", e)
        }
        finish()
    }

    private fun startTimer() {
        // Breathing: align captions to pulse animation
        // 0-4s (scale up) -> "Breathe in" ; 4-8s (scale down) -> "Breathe out"
        val totalSecs = (totalMillis / 1000).toInt()
        val cycleMs = 8000L // 4s in + 4s out
        val halfCycleMs = 4000L
        val startTime = System.currentTimeMillis()
        // Non-breathing category captions (rotate every 4s)
        val nonBreathingCaptions: Array<String> = when (category) {
            "Stretching" -> arrayOf(
                "Hold the stretch…",
                "Breathe gently",
                "Feel your muscles relax",
                "Release any tension",
                "Stay steady"
            )
            "Reflection" -> arrayOf(
                "Notice your thoughts",
                "Observe, don’t judge",
                "Be here now",
                "Take a gentle pause",
                "What are you feeling?"
            )
            "Journaling" -> arrayOf(
                "Write freely, don’t filter",
                "Let your thoughts flow",
                "Be honest with yourself",
                "Capture what you feel now",
                "Just a few mindful words…"
            )
            "Quick Games" -> arrayOf(
                "Stay present",
                "Enjoy the moment",
                "Don’t rush — just play",
                "Focus gently",
                "You’re doing great"
            )
            else -> arrayOf("Stay present")
        }
        var lastCaptionIndex = -1

        timer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                timerText.text = String.format("%d:%02d", secondsLeft / 60, secondsLeft % 60)
                // animate progress as time elapses (ring completes at 0)
                progress.progress = (totalSecs - secondsLeft)

                if (isBreathing) {
                    val elapsed = (System.currentTimeMillis() - startTime) % cycleMs
                    val caption = if (elapsed < halfCycleMs) "Breathe in" else "Breathe out"
                    instructionText.text = caption
                } else {
                    val tick = (System.currentTimeMillis() - startTime) / 4000L
                    val idx = (tick % nonBreathingCaptions.size).toInt()
                    if (idx != lastCaptionIndex) {
                        lastCaptionIndex = idx
                        instructionText.text = nonBreathingCaptions[idx]
                    }
                }
            }

            override fun onFinish() {
                instructionText.text = getString(R.string.completed)
                timerText.text = "0:00"
                progress.progress = totalSecs
                // small delay so user sees completion message
                if (!sessionRecorded) {
                    sessionRecorded = true
                    val endedAt = System.currentTimeMillis()
                    progressRepository.recordSession(
                        title = sessionTitle,
                        category = category,
                        minutes = sessionMinutes,
                        startedAtMillis = sessionStartedAtMillis,
                        endedAtMillis = endedAt,
                        completed = true
                    ) { _ -> }
                }
                instructionText.postDelayed({ finish() }, 800)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    companion object {
        private const val EXTRA_NAME = "name"
        private const val EXTRA_MINUTES = "minutes"
        private const val EXTRA_BREATHING = "breathing"
        private const val EXTRA_CATEGORY = "category"

        fun start(context: Context, name: String, minutes: Int, breathing: Boolean, category: String) {
            val i = Intent(context, ExerciseActivity::class.java)
            i.putExtra(EXTRA_NAME, name)
            i.putExtra(EXTRA_MINUTES, minutes)
            i.putExtra(EXTRA_BREATHING, breathing)
            i.putExtra(EXTRA_CATEGORY, category)
            context.startActivity(i)
        }
    }
}
