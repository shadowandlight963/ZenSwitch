package com.example.ourmajor.ui.breathing

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.gamification.ActivityType
import com.example.ourmajor.gamification.GamificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.button.MaterialButton

class SleepBreathActivity : AppCompatActivity() {

    private lateinit var sleepView: SleepBreathView
    private lateinit var timerText: TextView
    private lateinit var sessionRing: SessionRingView
    private lateinit var zenBackground: ZenBackgroundView
    private lateinit var completeOverlay: View

    private var sessionTimer: CountDownTimer? = null
    private var totalMillis: Long = 180_000L

    private var sessionStartedAtMillis: Long = 0L
    private var sessionRecorded: Boolean = false

    private var vibrator: Vibrator? = null

    private val progressRepository: ProgressRepository by lazy { ProgressRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: SleepBreathActivity")
        setContentView(R.layout.activity_sleep_breath)

        sleepView = findViewById(R.id.sleepBreathView)
        timerText = findViewById(R.id.hudTimerText)
        sessionRing = findViewById(R.id.sessionRing)
        zenBackground = findViewById(R.id.zenBackground)
        completeOverlay = findViewById(R.id.completeOverlay)

        val close = findViewById<ImageButton>(R.id.btnClose)
        val done = findViewById<MaterialButton>(R.id.btnDone)

        totalMillis = intent.getIntExtra(EXTRA_MINUTES, 3).coerceAtLeast(1) * 60_000L

        close.setOnClickListener { recordAndFinish(false) }
        done.setOnClickListener { finish() }

        completeOverlay.visibility = View.GONE

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }

        zenBackground.setStyle(ZenBackgroundView.Style.SLEEP)
        zenBackground.setPhase(ZenBackgroundView.Phase.INHALE)

        sleepView.setOnPhaseChangeListener(object : SleepBreathView.OnPhaseChangeListener {
            override fun onPhaseChanged(phase: SleepBreathView.Phase) {
                triggerHaptic(phase)

                val bgPhase = when (phase) {
                    SleepBreathView.Phase.INHALE -> ZenBackgroundView.Phase.INHALE
                    SleepBreathView.Phase.HOLD -> ZenBackgroundView.Phase.HOLD
                    SleepBreathView.Phase.EXHALE -> ZenBackgroundView.Phase.EXHALE
                }
                zenBackground.setPhase(bgPhase)
            }
        })

        sessionStartedAtMillis = System.currentTimeMillis()
        sleepView.resetVisual()
        sleepView.start()

        startSessionTimer()
    }

    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: SleepBreathActivity")
    }

    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: SleepBreathActivity")
    }

    private fun startSessionTimer() {
        updateTimerText(totalMillis)

        sessionTimer?.cancel()
        sessionTimer = object : CountDownTimer(totalMillis, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerText(millisUntilFinished)
                val done = (totalMillis - millisUntilFinished).coerceAtLeast(0L)
                val frac = if (totalMillis <= 0L) 0f else (done.toFloat() / totalMillis.toFloat())
                sessionRing.setProgress(frac)
            }

            override fun onFinish() {
                updateTimerText(0L)
                sessionRing.setProgress(1f)
                onSessionComplete()
            }
        }.start()
    }

    private fun onSessionComplete() {
        if (sessionRecorded) return
        sessionRecorded = true

        sleepView.stop()
        sleepView.fadeOut(700L)

        timerText.animate().alpha(0f).setDuration(450L).start()
        sessionRing.animate().alpha(0f).setDuration(450L).start()

        completeOverlay.alpha = 0f
        completeOverlay.visibility = View.VISIBLE
        completeOverlay.animate().alpha(1f).setDuration(300L).start()

        val endedAt = System.currentTimeMillis()
        try {
            val mins = (totalMillis / 60_000L).toInt().coerceAtLeast(1)
            progressRepository.recordSession(
                title = "4-7-8 Sleep Breath",
                category = "breathing",
                minutes = mins,
                startedAtMillis = sessionStartedAtMillis,
                endedAtMillis = endedAt,
                completed = true
            ) { }

            lifecycleScope.launch {
                runCatching {
                    val gm = GamificationManager.getInstance(this@SleepBreathActivity)
                    withContext(Dispatchers.IO) {
                        gm.awardPoints(
                            activityType = ActivityType.BREATHING,
                            duration = mins,
                            completedAtMillis = endedAt,
                            activityName = "4-7-8 Sleep Breath",
                            category = "Breathing"
                        )
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to record local sleep breath session", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record sleep breath session", e)
        }
    }

    private fun recordAndFinish(completed: Boolean) {
        if (sessionRecorded) {
            finish()
            return
        }
        sessionRecorded = true

        sleepView.stop()

        val endedAt = System.currentTimeMillis()
        try {
            progressRepository.recordSession(
                title = "4-7-8 Sleep Breath",
                category = "breathing",
                minutes = (totalMillis / 60_000L).toInt().coerceAtLeast(1),
                startedAtMillis = sessionStartedAtMillis,
                endedAtMillis = endedAt,
                completed = completed
            ) { }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record sleep breath session", e)
        }

        finish()
    }

    private fun triggerHaptic(phase: SleepBreathView.Phase) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (phase) {
                SleepBreathView.Phase.INHALE -> {
                    android.os.VibrationEffect.createOneShot(120, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                }
                SleepBreathView.Phase.HOLD -> {
                    android.os.VibrationEffect.createWaveform(longArrayOf(0, 35, 55, 35), -1)
                }
                SleepBreathView.Phase.EXHALE -> {
                    android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 120, 120, 120, 120, 120),
                        intArrayOf(0, 220, 180, 140, 90, 60),
                        -1
                    )
                }
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            when (phase) {
                SleepBreathView.Phase.INHALE -> v.vibrate(120)
                SleepBreathView.Phase.HOLD -> v.vibrate(longArrayOf(0, 35, 55, 35), -1)
                SleepBreathView.Phase.EXHALE -> v.vibrate(400)
            }
        }
    }

    private fun updateTimerText(millis: Long) {
        val s = (millis / 1000L).coerceAtLeast(0)
        val m = s / 60
        val rem = s % 60
        timerText.text = String.format("%d:%02d", m, rem)
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionTimer?.cancel()
        sleepView.stop()
    }

    companion object {
        private const val TAG = "SleepBreath"
        private const val EXTRA_MINUTES = "minutes"

        fun start(context: Context, minutes: Int) {
            val i = Intent(context, SleepBreathActivity::class.java)
            i.putExtra(EXTRA_MINUTES, minutes)
            context.startActivity(i)
        }
    }
}
