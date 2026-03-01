package com.example.ourmajor.ui.journaling

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.gamification.ActivityType
import com.example.ourmajor.gamification.GamificationManager
import com.example.ourmajor.gamification.RewardDialogs
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JournalingSessionActivity : AppCompatActivity() {

    enum class Mode(val id: String) {
        QUICK_REFLECTION("quick_reflection"),
        PRIORITY_CHECK("priority_check"),
        ENERGY_AUDIT("energy_audit");

        companion object {
            fun fromId(id: String?): Mode? = values().firstOrNull { it.id == id }
        }
    }

    private lateinit var tvTitle: TextView
    private lateinit var completeOverlay: View

    private val progressRepository: ProgressRepository by lazy { ProgressRepository() }

    private var startedAtMillis: Long = 0L
    private var sessionRecorded: Boolean = false

    private var sessionTitle: String = "Journaling"
    private var sessionMinutes: Int = 1
    private var sessionCategory: String = "journaling"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: JournalingSessionActivity")
        setContentView(R.layout.activity_journaling_session)

        tvTitle = findViewById(R.id.tvTitle)
        completeOverlay = findViewById(R.id.completeOverlay)

        val close = findViewById<ImageButton>(R.id.btnClose)
        val done = findViewById<MaterialButton>(R.id.btnDone)

        close.setOnClickListener { recordAndFinish(false) }
        done.setOnClickListener { finish() }

        completeOverlay.visibility = View.GONE

        val mode = Mode.fromId(intent.getStringExtra(EXTRA_MODE))
        sessionTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Journaling"
        sessionMinutes = intent.getIntExtra(EXTRA_MINUTES, 1).coerceAtLeast(1)

        tvTitle.text = sessionTitle
        startedAtMillis = System.currentTimeMillis()

        if (mode == null) {
            Log.e(TAG, "Missing/invalid journaling mode")
            finish()
            return
        }

        if (savedInstanceState == null) {
            val frag = when (mode) {
                Mode.QUICK_REFLECTION -> QuickReflectionFragment.newInstance(sessionTitle, sessionMinutes)
                Mode.PRIORITY_CHECK -> PriorityCheckFragment.newInstance(sessionTitle, sessionMinutes)
                Mode.ENERGY_AUDIT -> EnergyAuditFragment.newInstance(sessionTitle, sessionMinutes)
            }
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, frag)
            }
        }
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
            Log.e(TAG, "Failed to record journaling session", e)
        }

        lifecycleScope.launch {
            val gm = GamificationManager.getInstance(this@JournalingSessionActivity)
            val result = withContext(Dispatchers.IO) {
                gm.awardPoints(
                    activityType = ActivityType.JOURNALING,
                    duration = 1,
                    completedAtMillis = endedAt,
                    activityName = sessionTitle,
                    category = "Journaling"
                )
            }
            RewardDialogs.showRewardSequence(this@JournalingSessionActivity, result)
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
            Log.e(TAG, "Failed to record journaling session", e)
        }
        finish()
    }

    companion object {
        private const val TAG = "JournalingSession"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MINUTES = "minutes"

        fun start(context: Context, mode: Mode, title: String, minutes: Int) {
            val i = Intent(context, JournalingSessionActivity::class.java)
            i.putExtra(EXTRA_MODE, mode.id)
            i.putExtra(EXTRA_TITLE, title)
            i.putExtra(EXTRA_MINUTES, minutes)
            context.startActivity(i)
        }
    }
}
