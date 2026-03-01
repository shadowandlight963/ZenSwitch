package com.example.ourmajor.ui.stretching

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.data.stretching.StretchStep
import com.example.ourmajor.data.stretching.StretchingRoutines
import com.example.ourmajor.data.stretching.StretchingRoutine
import com.example.ourmajor.gamification.ActivityType
import com.example.ourmajor.gamification.GamificationManager
import com.example.ourmajor.gamification.RewardDialogs
import com.example.ourmajor.ui.breathing.SessionRingView
import com.example.ourmajor.ui.breathing.ZenBackgroundView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class StretchingSessionActivity : AppCompatActivity() {

    private lateinit var zenBackground: ZenBackgroundView
    private lateinit var tvRoutineTitle: TextView
    private lateinit var progressRoutine: ProgressBar
    private lateinit var tvStepName: TextView
    private lateinit var tvInstruction: TextView
    private lateinit var stepRing: SessionRingView
    private lateinit var tvStepSeconds: TextView
    private lateinit var tvStepCounter: TextView
    private lateinit var tvNextUp: TextView
    private lateinit var completeOverlay: View

    private lateinit var btnTtsMute: ImageButton

    private var routine: StretchingRoutine? = null

    private var currentStepIndex: Int = 0
    private var currentStepTimer: CountDownTimer? = null

    private var sessionStartedAtMillis: Long = 0L
    private var sessionRecorded: Boolean = false

    private val progressRepository: ProgressRepository by lazy { ProgressRepository() }

    private var vibrator: Vibrator? = null
    private var toneGen: ToneGenerator? = null

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private var ttsMuted: Boolean = false

    private var stepTotalMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: StretchingSessionActivity")
        setContentView(R.layout.activity_stretching_session)

        zenBackground = findViewById(R.id.zenBackground)
        tvRoutineTitle = findViewById(R.id.tvRoutineTitle)
        progressRoutine = findViewById(R.id.progressRoutine)
        tvStepName = findViewById(R.id.tvStepName)
        tvInstruction = findViewById(R.id.tvInstruction)
        stepRing = findViewById(R.id.stepRing)
        tvStepSeconds = findViewById(R.id.tvStepSeconds)
        tvStepCounter = findViewById(R.id.tvStepCounter)
        tvNextUp = findViewById(R.id.tvNextUp)
        completeOverlay = findViewById(R.id.completeOverlay)

        btnTtsMute = findViewById(R.id.btnTtsMute)

        val close = findViewById<ImageButton>(R.id.btnClose)
        val done = findViewById<MaterialButton>(R.id.btnDone)

        close.setOnClickListener { recordAndFinish(false) }
        done.setOnClickListener { finish() }

        completeOverlay.visibility = View.GONE

        zenBackground.setStyle(ZenBackgroundView.Style.SLEEP)
        zenBackground.setPhase(ZenBackgroundView.Phase.INHALE)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }

        toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)

        btnTtsMute.setOnClickListener {
            ttsMuted = !ttsMuted
            btnTtsMute.setImageResource(if (ttsMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
            if (ttsMuted) {
                tts?.stop()
            } else {
                routine?.steps?.getOrNull(currentStepIndex)?.let { speakStep(it) }
            }
        }

        setupTts()

        val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID).orEmpty()
        routine = StretchingRoutines.getRoutine(routineId)

        if (routine == null) {
            Log.e(TAG, "Unknown stretching routine id: $routineId")
            finish()
            return
        }

        sessionStartedAtMillis = System.currentTimeMillis()
        currentStepIndex = 0

        tvRoutineTitle.text = routine?.title ?: "Stretching"
        startStep(0)
    }

    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: StretchingSessionActivity")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: StretchingSessionActivity")
    }

    private fun setupTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                ttsReady = true

                engine.setSpeechRate(0.9f)

                val set = engine.setLanguage(Locale.getDefault())
                if (set == TextToSpeech.LANG_MISSING_DATA || set == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.US)
                }

                val voices = engine.voices?.toList().orEmpty()
                val preferred = voices.firstOrNull { v ->
                    val n = v.name.lowercase(Locale.US)
                    v.locale.language == Locale.getDefault().language &&
                        ("calm" in n || "soothing" in n || "female" in n || "en-us" in n)
                } ?: voices.firstOrNull { v ->
                    v.locale.language == Locale.getDefault().language
                }

                if (preferred != null) {
                    runCatching { engine.voice = preferred }
                }

                if (!ttsMuted) {
                    routine?.steps?.getOrNull(currentStepIndex)?.let { speakStep(it) }
                }
            } else {
                ttsReady = false
            }
        }
    }

    private fun startStep(index: Int) {
        val r = routine ?: return
        if (index !in r.steps.indices) {
            onRoutineComplete()
            return
        }

        currentStepTimer?.cancel()
        currentStepIndex = index

        val step = r.steps[index]
        bindStepUi(step, index, r.steps.size)

        stepTotalMillis = (step.durationSeconds.coerceAtLeast(1) * 1000L)
        updateStepTimeUi(stepTotalMillis)

        if (!ttsMuted) {
            speakStep(step)
        }

        currentStepTimer = object : CountDownTimer(stepTotalMillis, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                updateStepTimeUi(millisUntilFinished)
                updateOverallProgress(millisUntilFinished)
            }

            override fun onFinish() {
                updateStepTimeUi(0L)
                updateOverallProgress(0L)
                onStepFinished()
            }
        }.start()
    }

    private fun speakStep(step: StretchStep) {
        val engine = tts ?: return
        if (!ttsReady) return
        if (ttsMuted) return

        val utterance = if (step.isRest) {
            "${step.name}. Rest for ${step.durationSeconds} seconds."
        } else {
            step.instruction
        }

        runCatching {
            engine.stop()
            engine.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, "step_$currentStepIndex")
        }
    }

    private fun bindStepUi(step: StretchStep, index: Int, totalSteps: Int) {
        tvStepName.text = step.name
        tvInstruction.text = step.instruction
        tvStepCounter.text = "Step ${index + 1} of $totalSteps"

        val next = routine?.steps?.getOrNull(index + 1)
        tvNextUp.text = if (next != null) {
            "Next: ${next.name}"
        } else {
            "Next: Finish"
        }

        zenBackground.setPhase(if (step.isRest) ZenBackgroundView.Phase.HOLD else ZenBackgroundView.Phase.INHALE)

        if (step.isRest) {
            tvStepName.setTextColor(0xCCFFFFFF.toInt())
        } else {
            tvStepName.setTextColor(MaterialColors.getColor(tvStepName, com.google.android.material.R.attr.colorOnBackground))
        }
    }

    private fun updateOverallProgress(stepMillisRemaining: Long) {
        val r = routine ?: return
        val totalRoutineMillis = (r.totalDurationSeconds.coerceAtLeast(1) * 1000L)

        var completedBeforeThisStepMs = 0L
        for (i in 0 until currentStepIndex) {
            completedBeforeThisStepMs += (r.steps[i].durationSeconds * 1000L)
        }

        val currentStepTotalMs = (r.steps[currentStepIndex].durationSeconds.coerceAtLeast(1) * 1000L)
        val elapsedInStepMs = (currentStepTotalMs - stepMillisRemaining).coerceIn(0L, currentStepTotalMs)

        val elapsedTotalMs = (completedBeforeThisStepMs + elapsedInStepMs).coerceIn(0L, totalRoutineMillis)
        val frac = elapsedTotalMs.toFloat() / totalRoutineMillis.toFloat()

        progressRoutine.progress = (frac * progressRoutine.max).toInt().coerceIn(0, progressRoutine.max)
    }

    private fun onStepFinished() {
        playDingAndHaptic()
        zenBackground.setPhase(ZenBackgroundView.Phase.EXHALE)

        tvStepName.animate().alpha(0.0f).setDuration(130L).withEndAction {
            tvStepName.alpha = 1f
            startStep(currentStepIndex + 1)
        }.start()
    }

    private fun playDingAndHaptic() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
        } catch (_: Exception) {
        }

        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = android.os.VibrationEffect.createOneShot(60, 130)
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(60)
        }
    }

    private fun onRoutineComplete() {
        if (sessionRecorded) return
        sessionRecorded = true

        currentStepTimer?.cancel()
        tts?.stop()

        completeOverlay.alpha = 0f
        completeOverlay.visibility = View.VISIBLE
        completeOverlay.animate().alpha(1f).setDuration(300L).start()

        val endedAt = System.currentTimeMillis()
        val r = routine
        if (r != null) {
            try {
                val mins = ((r.totalDurationSeconds + 59) / 60).coerceAtLeast(1)
                progressRepository.recordSession(
                    title = r.title,
                    category = "stretching",
                    minutes = mins,
                    startedAtMillis = sessionStartedAtMillis,
                    endedAtMillis = endedAt,
                    completed = true
                ) { }

                lifecycleScope.launch {
                    val gm = GamificationManager.getInstance(this@StretchingSessionActivity)
                    val result = withContext(Dispatchers.IO) {
                        gm.awardPoints(
                            activityType = ActivityType.STRETCHING,
                            duration = mins,
                            completedAtMillis = endedAt,
                            activityName = r.title,
                            category = "Stretching"
                        )
                    }
                    RewardDialogs.showRewardSequence(this@StretchingSessionActivity, result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record stretching session", e)
            }
        }
    }

    private fun recordAndFinish(completed: Boolean) {
        if (sessionRecorded) {
            finish()
            return
        }
        sessionRecorded = true

        currentStepTimer?.cancel()
        tts?.stop()

        val endedAt = System.currentTimeMillis()
        val r = routine
        if (r != null) {
            try {
                val mins = ((r.totalDurationSeconds + 59) / 60).coerceAtLeast(1)
                progressRepository.recordSession(
                    title = r.title,
                    category = "stretching",
                    minutes = mins,
                    startedAtMillis = sessionStartedAtMillis,
                    endedAtMillis = endedAt,
                    completed = completed
                ) { }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record stretching session", e)
            }
        }

        finish()
    }

    private fun updateStepTimeUi(millis: Long) {
        val total = stepTotalMillis.coerceAtLeast(1L)
        val clamped = millis.coerceIn(0L, total)

        val fracRemaining = clamped.toFloat() / total.toFloat()
        stepRing.setProgress(fracRemaining)

        val s = ((clamped + 999L) / 1000L).toInt().coerceAtLeast(0)
        tvStepSeconds.text = s.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentStepTimer?.cancel()
        toneGen?.release()
        toneGen = null

        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val TAG = "StretchingSession"
        private const val EXTRA_ROUTINE_ID = "routine_id"

        fun start(context: Context, routineId: String) {
            val i = Intent(context, StretchingSessionActivity::class.java)
            i.putExtra(EXTRA_ROUTINE_ID, routineId)
            context.startActivity(i)
        }
    }
}
