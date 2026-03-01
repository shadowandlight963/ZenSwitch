package com.example.ourmajor.ui.mindfulness

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.commit
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.gamification.ActivityType
import com.example.ourmajor.gamification.GamificationManager
import com.example.ourmajor.ui.breathing.ZenBackgroundView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MindfulnessSessionActivity : AppCompatActivity() {

    enum class Mode(val id: String) {
        GRATITUDE("gratitude"),
        INTENTION("intention"),
        PAUSE("pause");

        companion object {
            fun fromId(id: String?): Mode? = values().firstOrNull { it.id == id }
        }
    }

    private lateinit var zenBackground: ZenBackgroundView
    private lateinit var tvTitle: TextView
    private lateinit var completeOverlay: View

    private val progressRepository: ProgressRepository by lazy { ProgressRepository() }

    private var startedAtMillis: Long = 0L
    private var sessionRecorded: Boolean = false

    private var sessionTitle: String = "Mindfulness"
    private var sessionMinutes: Int = 1
    private var sessionCategory: String = "mindfulness"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: MindfulnessSessionActivity")
        setContentView(R.layout.activity_mindfulness_session)

        zenBackground = findViewById(R.id.zenBackground)
        tvTitle = findViewById(R.id.tvTitle)
        completeOverlay = findViewById(R.id.completeOverlay)

        val close = findViewById<ImageButton>(R.id.btnClose)
        val done = findViewById<MaterialButton>(R.id.btnDone)

        close.setOnClickListener { recordAndFinish(false) }
        done.setOnClickListener { finish() }

        completeOverlay.visibility = View.GONE

        zenBackground.setStyle(ZenBackgroundView.Style.SLEEP)
        zenBackground.setPhase(ZenBackgroundView.Phase.INHALE)

        val mode = Mode.fromId(intent.getStringExtra(EXTRA_MODE))
        sessionTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Mindfulness"
        sessionMinutes = intent.getIntExtra(EXTRA_MINUTES, 1).coerceAtLeast(1)

        tvTitle.text = sessionTitle
        startedAtMillis = System.currentTimeMillis()

        if (mode == null) {
            Log.e(TAG, "Missing/invalid mindfulness mode")
            finish()
            return
        }

        if (savedInstanceState == null) {
            val frag = when (mode) {
                Mode.GRATITUDE -> GratitudeMomentFragment.newInstance(sessionTitle, sessionMinutes)
                Mode.INTENTION -> SetIntentionFragment.newInstance(sessionTitle, sessionMinutes)
                Mode.PAUSE -> MindfulPauseFragment.newInstance(sessionTitle, sessionMinutes)
            }
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, frag)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: MindfulnessSessionActivity")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: MindfulnessSessionActivity")
    }

    fun showCompleteOverlay() {
        if (completeOverlay.visibility == View.VISIBLE) return
        completeOverlay.alpha = 0f
        completeOverlay.visibility = View.VISIBLE
        completeOverlay.animate().alpha(1f).setDuration(300L).start()
    }

    fun recordSessionAndShowComplete() {
        if (sessionRecorded) {
            showCompleteOverlay()
            return
        }
        sessionRecorded = true

        val endedAt = System.currentTimeMillis()
        try {
            progressRepository.recordSession(
                title = sessionTitle,
                category = sessionCategory,
                minutes = sessionMinutes,
                startedAtMillis = startedAtMillis,
                endedAtMillis = endedAt,
                completed = true
            ) { }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record mindfulness session", e)
        }

        lifecycleScope.launch {
            runCatching {
                val gm = GamificationManager.getInstance(this@MindfulnessSessionActivity)
                withContext(Dispatchers.IO) {
                    gm.awardPoints(
                        activityType = ActivityType.MINDFULNESS,
                        duration = sessionMinutes,
                        completedAtMillis = endedAt,
                        activityName = sessionTitle,
                        category = "Mindfulness"
                    )
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to record local mindfulness session", e)
            }
        }

        showCompleteOverlay()
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
                category = sessionCategory,
                minutes = sessionMinutes,
                startedAtMillis = startedAtMillis,
                endedAtMillis = endedAt,
                completed = completed
            ) { }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record mindfulness session", e)
        }
        finish()
    }

    companion object {
        private const val TAG = "MindfulnessSession"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MINUTES = "minutes"

        fun start(context: Context, mode: Mode, title: String, minutes: Int) {
            val i = Intent(context, MindfulnessSessionActivity::class.java)
            i.putExtra(EXTRA_MODE, mode.id)
            i.putExtra(EXTRA_TITLE, title)
            i.putExtra(EXTRA_MINUTES, minutes)
            context.startActivity(i)
        }
    }
}
