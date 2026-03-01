package com.example.ourmajor.ui.breathing

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.gamification.ActivityType
import com.example.ourmajor.gamification.GamificationManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResonantBreathActivity : AppCompatActivity() {

    private lateinit var waveView: WaveBreathView
    private lateinit var timerText: TextView
    private lateinit var sessionRing: SessionRingView
    private lateinit var zenBackground: ZenBackgroundView
    private lateinit var soundSwitch: SwitchMaterial
    private lateinit var completeOverlay: View

    private var sessionTimer: CountDownTimer? = null
    private var totalMillis: Long = 60_000L

    private var sessionStartedAtMillis: Long = 0L
    private var sessionRecorded: Boolean = false

    private val progressRepository: ProgressRepository by lazy { ProgressRepository() }

    private var vibrator: Vibrator? = null

    private var muted: Boolean = false

    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var soundLoaded: Boolean = false
    private var soundStreamId: Int = 0

    private var oceanEngine: OceanAudioEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: ResonantBreathActivity")
        setContentView(R.layout.activity_resonant_breath)

        waveView = findViewById(R.id.waveBreathView)
        timerText = findViewById(R.id.hudTimerText)
        sessionRing = findViewById(R.id.sessionRing)
        zenBackground = findViewById(R.id.zenBackground)
        soundSwitch = findViewById(R.id.soundSwitch)
        completeOverlay = findViewById(R.id.completeOverlay)

        val close = findViewById<ImageButton>(R.id.btnClose)
        val done = findViewById<MaterialButton>(R.id.btnDone)

        totalMillis = intent.getIntExtra(EXTRA_MINUTES, 1).coerceAtLeast(1) * 60_000L

        close.setOnClickListener { recordAndFinish(false) }
        done.setOnClickListener { finish() }

        completeOverlay.visibility = View.GONE

        soundSwitch.isChecked = true
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            muted = !isChecked
            if (muted) {
                stopOceanLoop()
            } else {
                startOceanLoopIfReady()
            }
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }

        zenBackground.setStyle(ZenBackgroundView.Style.WAVE)
        zenBackground.setPhase(ZenBackgroundView.Phase.INHALE)

        setupAudio()
        setupWaveCallbacks()

        sessionStartedAtMillis = System.currentTimeMillis()
        waveView.resetVisual()
        waveView.start()

        startOceanLoopIfReady()
        startSessionTimer()
    }

    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: ResonantBreathActivity")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: ResonantBreathActivity")
    }

    private fun setupWaveCallbacks() {
        waveView.setListener(object : WaveBreathView.Listener {
            override fun onPhaseChanged(phase: WaveBreathView.Phase) {
                triggerHaptic(phase)

                val bgPhase = when (phase) {
                    WaveBreathView.Phase.INHALE -> ZenBackgroundView.Phase.INHALE
                    WaveBreathView.Phase.EXHALE -> ZenBackgroundView.Phase.EXHALE
                }
                zenBackground.setPhase(bgPhase)
            }

            override fun onProgress(progress: Float, phase: WaveBreathView.Phase) {
                val vol = when (phase) {
                    WaveBreathView.Phase.INHALE -> (progress / 0.5f).coerceIn(0f, 1f)
                    WaveBreathView.Phase.EXHALE -> (1f - ((progress - 0.5f) / 0.5f)).coerceIn(0f, 1f)
                }
                setOceanVolume(vol)
            }
        })
    }

    private fun setupAudio() {
        soundPool = SoundPool.Builder().setMaxStreams(1).build()

        val resId = resources.getIdentifier("ocean_wave", "raw", packageName)
        if (resId == 0) {
            Log.w(TAG, "R.raw.ocean_wave not found. Using procedural ocean audio fallback.")
            oceanEngine = OceanAudioEngine()
            return
        }

        soundId = soundPool?.load(this, resId, 1) ?: 0
        if (soundId == 0) {
            Log.w(TAG, "Failed to load ocean_wave. Using procedural ocean audio fallback.")
            oceanEngine = OceanAudioEngine()
            return
        }

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == soundId && status == 0) {
                soundLoaded = true
                startOceanLoopIfReady()
            }
        }
    }

    private fun startOceanLoopIfReady() {
        if (muted) return
        oceanEngine?.start()

        val sp = soundPool ?: return
        if (!soundLoaded || soundId == 0) return
        if (soundStreamId != 0) return

        soundStreamId = sp.play(soundId, 0f, 0f, 0, -1, 1f)
        if (soundStreamId == 0) {
            Log.w(TAG, "Failed to start looping ocean_wave.")
        }
    }

    private fun stopOceanLoop() {
        val sp = soundPool ?: return
        if (soundStreamId != 0) {
            sp.stop(soundStreamId)
            soundStreamId = 0
        }

        oceanEngine?.stop()
    }

    private fun setOceanVolume(vol: Float) {
        if (muted) return
        val v = vol.coerceIn(0f, 1f)
        oceanEngine?.setVolume(v)

        val sp = soundPool ?: return
        if (soundStreamId == 0) return
        try {
            sp.setVolume(soundStreamId, v, v)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set ocean volume", e)
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

        waveView.stop()
        waveView.fadeOut(600L)
        stopOceanLoop()

        timerText.animate().alpha(0f).setDuration(450L).start()
        sessionRing.animate().alpha(0f).setDuration(450L).start()

        completeOverlay.alpha = 0f
        completeOverlay.visibility = View.VISIBLE
        completeOverlay.animate().alpha(1f).setDuration(300L).start()

        val endedAt = System.currentTimeMillis()
        try {
            val mins = (totalMillis / 60_000L).toInt().coerceAtLeast(1)
            progressRepository.recordSession(
                title = "Resonant Wave",
                category = "breathing",
                minutes = mins,
                startedAtMillis = sessionStartedAtMillis,
                endedAtMillis = endedAt,
                completed = true
            ) { }

            lifecycleScope.launch {
                runCatching {
                    val gm = GamificationManager.getInstance(this@ResonantBreathActivity)
                    withContext(Dispatchers.IO) {
                        gm.awardPoints(
                            activityType = ActivityType.BREATHING,
                            duration = mins,
                            completedAtMillis = endedAt,
                            activityName = "Resonant Wave",
                            category = "Breathing"
                        )
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to record local resonant breath session", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record resonant breath session", e)
        }
    }

    private fun recordAndFinish(completed: Boolean) {
        if (sessionRecorded) {
            finish()
            return
        }
        sessionRecorded = true

        waveView.stop()
        stopOceanLoop()

        val endedAt = System.currentTimeMillis()
        try {
            progressRepository.recordSession(
                title = "Resonant Wave",
                category = "breathing",
                minutes = (totalMillis / 60_000L).toInt().coerceAtLeast(1),
                startedAtMillis = sessionStartedAtMillis,
                endedAtMillis = endedAt,
                completed = completed
            ) { }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record resonant breath session", e)
        }

        finish()
    }

    private fun triggerHaptic(phase: WaveBreathView.Phase) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (phase) {
                WaveBreathView.Phase.INHALE -> android.os.VibrationEffect.createOneShot(45, 90)
                WaveBreathView.Phase.EXHALE -> android.os.VibrationEffect.createOneShot(35, 60)
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            when (phase) {
                WaveBreathView.Phase.INHALE -> v.vibrate(45)
                WaveBreathView.Phase.EXHALE -> v.vibrate(35)
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
        waveView.stop()
        stopOceanLoop()

        soundPool?.release()
        soundPool = null

        oceanEngine?.release()
        oceanEngine = null
    }

    private class OceanAudioEngine {
        private val sampleRate = 22050
        private val bufferFrames = 512

        @Volatile private var running: Boolean = false
        @Volatile private var volume: Float = 0f

        private var thread: Thread? = null
        private var track: AudioTrack? = null

        fun start() {
            if (running) return
            running = true

            val minBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferFrames * 2)

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track = audioTrack
            audioTrack.play()

            thread = Thread {
                val buf = ShortArray(bufferFrames)
                var seed = 0x1234ABCD.toInt()
                var last = 0f
                var phase = 0.0
                val twoPi = (2.0 * Math.PI)
                val toneHz = 90.0
                val phaseStep = twoPi * toneHz / sampleRate.toDouble()

                while (running) {
                    val vol = volume.coerceIn(0f, 1f)
                    for (i in 0 until bufferFrames) {
                        seed = seed * 1664525 + 1013904223
                        val rnd = ((seed ushr 9) and 0x7FFF) / 16384f - 1f
                        last += 0.02f * (rnd - last)

                        val sine = kotlin.math.sin(phase).toFloat() * 0.12f
                        phase += phaseStep
                        if (phase > twoPi) phase -= twoPi

                        val mixed = (last * 0.45f + sine)
                        val sample = (mixed * vol * 32767f).toInt().coerceIn(-32767, 32767)
                        buf[i] = sample.toShort()
                    }

                    try {
                        audioTrack.write(buf, 0, buf.size)
                    } catch (_: Exception) {
                        // If write fails, stop to avoid busy looping.
                        running = false
                    }
                }

                try {
                    audioTrack.pause()
                    audioTrack.flush()
                } catch (_: Exception) {
                }
            }.apply { name = "OceanAudioEngine" }

            thread?.start()
        }

        fun stop() {
            running = false
        }

        fun setVolume(v: Float) {
            volume = v.coerceIn(0f, 1f)
        }

        fun release() {
            running = false
            try {
                thread?.join(250)
            } catch (_: InterruptedException) {
            }
            thread = null

            try {
                track?.stop()
            } catch (_: Exception) {
            }
            try {
                track?.release()
            } catch (_: Exception) {
            }
            track = null
        }
    }

    companion object {
        private const val TAG = "ResonantBreath"
        private const val EXTRA_MINUTES = "minutes"

        fun start(context: Context, minutes: Int) {
            val i = Intent(context, ResonantBreathActivity::class.java)
            i.putExtra(EXTRA_MINUTES, minutes)
            context.startActivity(i)
        }
    }
}
