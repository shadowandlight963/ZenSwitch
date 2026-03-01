package com.example.ourmajor.ui.quickgames

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.HapticFeedbackConstants
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.gamification.ActivityType
import com.example.ourmajor.gamification.GamificationManager
import com.example.ourmajor.gamification.RewardDialogs
import com.example.ourmajor.ui.breathing.ZenBackgroundView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickGamesActivity : AppCompatActivity() {

    enum class Mode(val id: String) {
        MEMORY_MATRIX("memory_matrix"),
        BUBBLE_FOCUS("bubble_focus"),
        ZEN_SCRAMBLE("zen_scramble");

        companion object {
            fun fromId(id: String?): Mode? = values().firstOrNull { it.id == id }
        }
    }

    private lateinit var root: View
    private lateinit var zenBackground: ZenBackgroundView
    private lateinit var tvTitle: TextView
    private lateinit var completeOverlay: View
    private lateinit var tvCompleteTitle: TextView
    private lateinit var tvCompleteSubtitle: TextView

    lateinit var audio: QuickGameAudio
        private set

    private val progressRepository: ProgressRepository by lazy { ProgressRepository() }

    private var startedAtMillis: Long = 0L
    private var sessionRecorded: Boolean = false

    private var sessionTitle: String = "Quick Games"
    private var sessionMinutes: Int = 1
    private var sessionCategory: String = "quick_games"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ACTIVITY_DEBUG", "Launched: QuickGamesActivity")
        setContentView(R.layout.activity_quick_games)

        enterImmersiveMode()

        audio = QuickGameAudio(this)

        root = findViewById(R.id.root)
        zenBackground = findViewById(R.id.zenBackground)
        tvTitle = findViewById(R.id.tvTitle)
        completeOverlay = findViewById(R.id.completeOverlay)
        tvCompleteTitle = findViewById(R.id.tvCompleteTitle)
        tvCompleteSubtitle = findViewById(R.id.tvCompleteSubtitle)

        val quit = findViewById<ImageButton>(R.id.btnQuit)
        val done = findViewById<MaterialButton>(R.id.btnDone)

        quit.setOnClickListener { recordAndFinish(false) }
        done.setOnClickListener { finish() }

        completeOverlay.visibility = View.GONE

        zenBackground.setStyle(ZenBackgroundView.Style.FOCUS)
        zenBackground.setPhase(ZenBackgroundView.Phase.INHALE)

        val mode = Mode.fromId(intent.getStringExtra(EXTRA_MODE))
        sessionTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Quick Games"
        sessionMinutes = intent.getIntExtra(EXTRA_MINUTES, 1).coerceAtLeast(1)

        tvTitle.text = sessionTitle
        startedAtMillis = System.currentTimeMillis()

        if (mode == null) {
            Log.e(TAG, "Missing/invalid quick game mode")
            finish()
            return
        }

        applyModeLook(mode)

        if (savedInstanceState == null) {
            val frag = when (mode) {
                Mode.MEMORY_MATRIX -> MemoryMatrixFragment.newInstance(sessionTitle, sessionMinutes)
                Mode.BUBBLE_FOCUS -> BubbleFocusFragment.newInstance(sessionTitle, sessionMinutes)
                Mode.ZEN_SCRAMBLE -> ZenScrambleFragment.newInstance(sessionTitle, sessionMinutes)
            }
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, frag)
            }
        }
    }

    private fun applyModeLook(mode: Mode) {
        root.setBackgroundColor(Color.BLACK)
        when (mode) {
            Mode.MEMORY_MATRIX -> {
                // Max contrast for neon flashes.
                zenBackground.visibility = View.GONE
            }
            else -> {
                zenBackground.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            audio.release()
        } catch (_: Exception) {
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onStart() {
        super.onStart()
        Log.d("ACTIVITY_DEBUG", "onStart: QuickGamesActivity")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("ACTIVITY_DEBUG", "onResume: QuickGamesActivity")
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun hapticLight(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun hapticHeavy(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun showCompleteOverlay(title: String, subtitle: String) {
        tvCompleteTitle.text = title
        tvCompleteSubtitle.text = subtitle
        if (completeOverlay.visibility == View.VISIBLE) return
        completeOverlay.alpha = 0f
        completeOverlay.visibility = View.VISIBLE
        completeOverlay.animate().alpha(1f).setDuration(280L).start()
    }

    fun recordSessionAndShowComplete(title: String, subtitle: String, gameScore: Int = 0) {
        if (sessionRecorded) {
            showCompleteOverlay(title, subtitle)
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
            Log.e(TAG, "Failed to record quick game session", e)
        }

        lifecycleScope.launch {
            val gm = GamificationManager.getInstance(this@QuickGamesActivity)
            val result = withContext(Dispatchers.IO) {
                gm.awardPoints(
                    activityType = ActivityType.GAMES,
                    duration = sessionMinutes,
                    gameScore = gameScore,
                    completedAtMillis = endedAt,
                    activityName = sessionTitle,
                    category = "Games"
                )
            }
            RewardDialogs.showRewardSequence(this@QuickGamesActivity, result)
        }

        showCompleteOverlay(title, subtitle)
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
            Log.e(TAG, "Failed to record quick game session", e)
        }
        finish()
    }

    companion object {
        private const val TAG = "QuickGames"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MINUTES = "minutes"

        fun start(context: Context, mode: Mode, title: String, minutes: Int) {
            val i = Intent(context, QuickGamesActivity::class.java)
            i.putExtra(EXTRA_MODE, mode.id)
            i.putExtra(EXTRA_TITLE, title)
            i.putExtra(EXTRA_MINUTES, minutes)
            context.startActivity(i)
        }
    }
}
