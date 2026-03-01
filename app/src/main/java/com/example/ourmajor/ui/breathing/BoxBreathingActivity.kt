package com.example.ourmajor.ui.breathing

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibratorManager
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.gamification.ActivityType
import com.example.ourmajor.gamification.GamificationManager
import com.example.ourmajor.gamification.RewardDialogs
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BoxBreathingActivity : AppCompatActivity() {

    private lateinit var breathingView: BoxBreathingView
    private lateinit var timerText: TextView
    private lateinit var sessionRing: SessionRingView
    private lateinit var zenBackground: ZenBackgroundView
    private lateinit var soundSwitch: SwitchMaterial
    private lateinit var completeOverlay: View

    private var sessionTimer: CountDownTimer? = null
    private var totalMillis: Long = 120_000L
    private var sessionStartedAtMillis: Long = 0L
    private var sessionRecorded: Boolean = false

    private val progressRepository: ProgressRepository by lazy { ProgressRepository() }

    private var muted: Boolean = false

    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var soundLoaded: Boolean = false
    private var toneGen: ToneGenerator? = null

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: BoxBreathingActivity")
        setContentView(R.layout.activity_box_breathing)

        breathingView = findViewById(R.id.boxBreathingView)
        timerText = findViewById(R.id.hudTimerText)
        sessionRing = findViewById(R.id.sessionRing)
        zenBackground = findViewById(R.id.zenBackground)
        soundSwitch = findViewById(R.id.soundSwitch)
        completeOverlay = findViewById(R.id.completeOverlay)

        val close = findViewById<ImageButton>(R.id.btnClose)
        val done = findViewById<MaterialButton>(R.id.btnDone)

        totalMillis = intent.getIntExtra(EXTRA_MINUTES, 2).coerceAtLeast(1) * 60_000L

        close.setOnClickListener { recordAndFinish(false) }
        done.setOnClickListener { finish() }

        completeOverlay.visibility = View.GONE

        soundSwitch.isChecked = true
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            muted = !isChecked
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }

        zenBackground.setStyle(ZenBackgroundView.Style.FOCUS)
        zenBackground.setPhase(ZenBackgroundView.Phase.INHALE)

        setupBreathingCallbacks()

        sessionStartedAtMillis = System.currentTimeMillis()
        breathingView.resetVisual()
        breathingView.start()

        startSessionTimer()
    }

    private fun setupBreathingCallbacks() {
        breathingView.setOnPhaseChangeListener(object : BoxBreathingView.OnPhaseChangeListener {
            override fun onPhaseChanged(phase: BoxBreathingView.Phase) {
                triggerHaptic(phase)
                triggerSound()

                val bgPhase = when (phase) {
                    BoxBreathingView.Phase.INHALE -> ZenBackgroundView.Phase.INHALE
                    BoxBreathingView.Phase.EXHALE -> ZenBackgroundView.Phase.EXHALE
                    BoxBreathingView.Phase.HOLD_1, BoxBreathingView.Phase.HOLD_2 -> ZenBackgroundView.Phase.HOLD
                }
                zenBackground.setPhase(bgPhase)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: BoxBreathingActivity")
    }

    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: BoxBreathingActivity")
        setupUI()
    }

    private fun setupUI() {
        toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .build()

        val resId = resources.getIdentifier("breathing_cue", "raw", packageName)
        if (resId == 0) {
            Log.w(TAG, "R.raw.breathing_cue not found. Using ToneGenerator fallback.")
            return
        }

        soundId = soundPool?.load(this, resId, 1) ?: 0
        if (soundId == 0) {
            Log.w(TAG, "Failed to load breathing_cue. Using ToneGenerator fallback.")
            return
        }

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == soundId && status == 0) {
                soundLoaded = true
            }
        }
    }

    private fun triggerSound() {
        if (muted) return

        val sp = soundPool
        if (sp != null && soundId != 0 && soundLoaded) {
            sp.play(soundId, 0.7f, 0.7f, 0, 0, 1f)
        } else {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        }
    }

    private fun triggerHaptic(phase: BoxBreathingView.Phase) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (phase) {
                BoxBreathingView.Phase.INHALE, BoxBreathingView.Phase.EXHALE -> {
                    android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                }
                BoxBreathingView.Phase.HOLD_1, BoxBreathingView.Phase.HOLD_2 -> {
                    android.os.VibrationEffect.createWaveform(longArrayOf(0, 35, 60, 35), -1)
                }
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            when (phase) {
                BoxBreathingView.Phase.INHALE, BoxBreathingView.Phase.EXHALE -> v.vibrate(100)
                BoxBreathingView.Phase.HOLD_1, BoxBreathingView.Phase.HOLD_2 -> v.vibrate(longArrayOf(0, 35, 60, 35), -1)
            }
        }
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

        breathingView.stop()
        breathingView.fadeOut(600L)

        timerText.animate().alpha(0f).setDuration(450L).start()
        sessionRing.animate().alpha(0f).setDuration(450L).start()

        completeOverlay.alpha = 0f
        completeOverlay.visibility = View.VISIBLE
        completeOverlay.animate().alpha(1f).setDuration(300L).start()

        val endedAt = System.currentTimeMillis()
        try {
            val mins = (totalMillis / 60_000L).toInt().coerceAtLeast(1)
            progressRepository.recordSession(
                title = "Box Breathing",
                category = "breathing",
                minutes = mins,
                startedAtMillis = sessionStartedAtMillis,
                endedAtMillis = endedAt,
                completed = true
            ) { }

            lifecycleScope.launch {
                val gm = GamificationManager.getInstance(this@BoxBreathingActivity)
                val result = withContext(Dispatchers.IO) {
                    gm.awardPoints(
                        activityType = ActivityType.BREATHING,
                        duration = mins,
                        completedAtMillis = endedAt,
                        activityName = "Box Breathing",
                        category = "Breathing"
                    )
                }
                RewardDialogs.showRewardSequence(this@BoxBreathingActivity, result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record breathing session", e)
        }
    }

    private fun recordAndFinish(completed: Boolean) {
        if (sessionRecorded) {
            finish()
            return
        }
        sessionRecorded = true

        breathingView.stop()

        val endedAt = System.currentTimeMillis()
        try {
            progressRepository.recordSession(
                title = "Box Breathing",
                category = "breathing",
                minutes = (totalMillis / 60_000L).toInt().coerceAtLeast(1),
                startedAtMillis = sessionStartedAtMillis,
                endedAtMillis = endedAt,
                completed = completed
            ) { }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record breathing session", e)
        }

        finish()
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
        breathingView.stop()

        soundPool?.release()
        soundPool = null
        toneGen?.release()
        toneGen = null
    }

    companion object {
        private const val TAG = "BoxBreathing"
        private const val EXTRA_MINUTES = "minutes"

        fun start(context: Context, minutes: Int) {
            val i = Intent(context, BoxBreathingActivity::class.java)
            i.putExtra(EXTRA_MINUTES, minutes)
            context.startActivity(i)
        }
    }
}
