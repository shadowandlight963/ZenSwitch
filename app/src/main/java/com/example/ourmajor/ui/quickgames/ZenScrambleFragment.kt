package com.example.ourmajor.ui.quickgames

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ourmajor.R
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

class ZenScrambleFragment : Fragment(R.layout.fragment_zen_scramble) {

    private lateinit var playArea: FrameLayout
    private lateinit var stressBar: ProgressBar
    private lateinit var tvWordsSolved: TextView

    private lateinit var lettersContainer: LinearLayout
    private lateinit var slotsContainer: LinearLayout
    private lateinit var tileLayer: FrameLayout
    private lateinit var sunburst: SunburstView
    private lateinit var tvResult: TextView

    private val ui = Handler(Looper.getMainLooper())

    private val words = listOf("CALM", "HOPE", "JOY", "FOCUS", "PEACE", "BREATH", "SMILE")

    private var target: String = ""
    private val slotViews = ArrayList<TextView>()
    private val slotCenters = ArrayList<FloatArray>()
    private val tileViews = ArrayList<MaterialButton>()
    private val placed = HashMap<Int, Char>()
    private var lockInput: Boolean = false

    private var timeLeftMs: Long = 60_000L
    private var wordsSolved: Int = 0
    private var slideFromRight: Boolean = true
    private val timerTick = object : Runnable {
        override fun run() {
            if (!isAdded) return
            if (timeLeftMs <= 0L) {
                endTimeAttack()
                return
            }
            timeLeftMs = (timeLeftMs - 50L).coerceAtLeast(0L)
            updateTimerUi()
            ui.postDelayed(this, 50L)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playArea = view.findViewById(R.id.playArea)
        stressBar = view.findViewById(R.id.stressBar)
        tvWordsSolved = view.findViewById(R.id.tvWordsSolved)

        lettersContainer = view.findViewById(R.id.lettersContainer)
        slotsContainer = view.findViewById(R.id.slotsContainer)
        tileLayer = view.findViewById(R.id.tileLayer)
        sunburst = view.findViewById(R.id.sunburst)
        tvResult = view.findViewById(R.id.tvResult)

        lettersContainer.visibility = View.GONE

        startTimeAttack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ui.removeCallbacksAndMessages(null)
    }

    private fun startTimeAttack() {
        wordsSolved = 0
        timeLeftMs = 60_000L
        slideFromRight = true
        stressBar.max = 60_000
        updateTimerUi()
        updateScoreUi()
        nextWordFast(animate = false)
        ui.post(timerTick)
    }

    private fun updateTimerUi() {
        stressBar.progress = timeLeftMs.toInt().coerceIn(0, stressBar.max)
        val pct = (timeLeftMs / 60_000f).coerceIn(0f, 1f)
        stressBar.scaleX = 0.15f + (0.85f * pct)
        stressBar.alpha = 0.85f
    }

    private fun updateScoreUi() {
        tvWordsSolved.text = "Words Solved: $wordsSolved"
    }

    private fun endTimeAttack() {
        lockInput = true
        ui.removeCallbacks(timerTick)
        tileViews.forEach { it.isEnabled = false }
        (activity as? QuickGamesActivity)?.recordSessionAndShowComplete(
            title = "Time!",
            subtitle = "Words solved: $wordsSolved",
            gameScore = wordsSolved * 10
        )
    }

    private fun nextWordFast(animate: Boolean) {
        if (timeLeftMs <= 0L) {
            endTimeAttack()
            return
        }
        tvResult.text = ""
        target = words.random(Random(System.nanoTime()))
        buildUIForWord(target)
        if (animate) {
            playArea.animate().cancel()
            val start = if (slideFromRight) dpF(90f) else -dpF(90f)
            playArea.translationX = start
            playArea.alpha = 0.92f
            playArea.animate().translationX(0f).alpha(1f).setDuration(160L).start()
            slideFromRight = !slideFromRight
        } else {
            playArea.translationX = 0f
            playArea.alpha = 1f
        }
    }

    private fun buildUIForWord(word: String) {
        lettersContainer.removeAllViews()
        slotsContainer.removeAllViews()
        tileLayer.removeAllViews()
        tileViews.clear()
        slotViews.clear()
        slotCenters.clear()
        placed.clear()
        lockInput = false

        slotsContainer.alpha = 1f

        val letters = word.toList().shuffled(Random(System.nanoTime()))

        repeat(word.length) {
            val slot = TextView(requireContext()).apply {
                text = "_"
                textSize = 22f
                setTextColor(0xEEFFFFFF.toInt())
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(10)
            }
            slotsContainer.addView(slot, lp)
            slotViews.add(slot)
        }

        // Place draggable tiles inside the play area with a tactile texture.
        tileLayer.post {
            computeSlotCenters()
            spawnTiles(letters)
        }
    }

    private fun checkWord() {
        val built = buildString {
            for (i in 0 until target.length) {
                val c = placed[i] ?: return
                append(c)
            }
        }

        if (built == target) {
            lockInput = true
            wordsSolved += 1
            timeLeftMs = (timeLeftMs + 5_000L).coerceAtMost(60_000L)
            updateTimerUi()
            updateScoreUi()

            (activity as? QuickGamesActivity)?.audio?.playDing()
            view?.let { (activity as? QuickGamesActivity)?.hapticHeavy(it) }

            sunburst.visibility = View.VISIBLE
            sunburst.burst()
            tvResult.text = "+5s"

            explodeTiles()
            ui.postDelayed({
                sunburst.visibility = View.GONE
                nextWordFast(animate = true)
            }, 220L)
        } else {
            (activity as? QuickGamesActivity)?.audio?.playDing()
            tvResult.text = "Try again"
            shake(slotsContainer)
            ui.postDelayed({ resetCurrent() }, 650L)
        }
    }

    private fun explodeTiles() {
        val rand = Random(System.nanoTime())
        tileViews.forEach { t ->
            val dx = (-dpF(90f) + rand.nextFloat() * dpF(180f))
            val dy = (-dpF(140f) + rand.nextFloat() * dpF(60f))
            val rot = (-18f + rand.nextFloat() * 36f)
            t.animate()
                .translationX(t.translationX + dx)
                .translationY(t.translationY + dy)
                .rotationBy(rot)
                .alpha(0f)
                .setDuration(180L)
                .start()
        }
        slotsContainer.animate().alpha(0.0f).setDuration(160L).start()
    }

    private fun resetCurrent() {
        slotViews.forEach { it.text = "_" }
        placed.clear()
        lockInput = false
        tileViews.forEach { t ->
            t.isEnabled = true
            t.alpha = 1f
            t.tag = null
            t.animate().cancel()
            t.rotation = 0f
        }
        glowSlots(false)
        tvResult.text = ""
    }

    private fun glowSlots(on: Boolean) {
        val c = if (on) 0xFFFFD666.toInt() else 0xEEFFFFFF.toInt()
        slotViews.forEach { it.setTextColor(c) }
    }

    private fun goldifyTiles(on: Boolean) {
        val tint = if (on) 0xFFFFD666.toInt() else 0xFFFFFFFF.toInt()
        tileViews.forEach { it.setTextColor(tint) }
        glowSlots(on)
    }

    private fun computeSlotCenters() {
        slotCenters.clear()
        val layerLoc = IntArray(2)
        tileLayer.getLocationOnScreen(layerLoc)

        slotViews.forEach { slot ->
            val loc = IntArray(2)
            slot.getLocationOnScreen(loc)
            val cx = (loc[0] - layerLoc[0]) + slot.width / 2f
            val cy = (loc[1] - layerLoc[1]) + slot.height / 2f
            slotCenters.add(floatArrayOf(cx, cy))
        }
    }

    private fun spawnTiles(letters: List<Char>) {
        val w = tileLayer.width
        val h = tileLayer.height
        if (w <= 0 || h <= 0) return

        val tileSize = dp(54)
        val spacing = dp(12)

        // Arrange tiles near the top, centered.
        val totalW = letters.size * tileSize + (letters.size - 1) * spacing
        var x = (w - totalW) / 2
        val y = dp(18)

        letters.forEachIndexed { index, ch ->
            val btn = MaterialButton(requireContext()).apply {
                text = ch.toString()
                isAllCaps = false
                textSize = 18f
                setTextColor(0xEEFFFFFF.toInt())
                background = resources.getDrawable(if (index % 2 == 0) R.drawable.bg_stone_tile else R.drawable.bg_wood_tile, null)
                cornerRadius = dp(18)
                setPadding(0, 0, 0, 0)
                elevation = dpF(2f)
            }

            val lp = FrameLayout.LayoutParams(tileSize, tileSize)
            tileLayer.addView(btn, lp)
            btn.translationX = x.toFloat()
            btn.translationY = y.toFloat()
            tileViews.add(btn)

            btn.setOnTouchListener(DragTouch(btn, ch))

            x += tileSize + spacing
        }
    }

    private inner class DragTouch(
        private val tile: MaterialButton,
        private val letter: Char
    ) : View.OnTouchListener {

        private var downX = 0f
        private var downY = 0f
        private var startX = 0f
        private var startY = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (lockInput) return true
            if (!tile.isEnabled) return true

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = tile.translationX
                    startY = tile.translationY
                    tile.bringToFront()
                    tile.animate().scaleX(1.06f).scaleY(1.06f).setDuration(90L).start()
                    view?.let { (activity as? QuickGamesActivity)?.hapticLight(it) }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    tile.translationX = startX + dx
                    tile.translationY = startY + dy
                    maybeMagnet(tile)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    tile.animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                    val snapped = trySnap(tile, letter)
                    if (!snapped) {
                        // Float back gently if not placed.
                        tile.animate().translationX(startX).translationY(startY).setDuration(180L).start()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun maybeMagnet(tile: MaterialButton) {
        // subtle tick when close to a free slot
        val idx = nearestFreeSlot(tile.centerX(), tile.centerY())
        if (idx == null) return
        val c = slotCenters[idx]
        val dx = c[0] - tile.centerX()
        val dy = c[1] - tile.centerY()
        val dist2 = dx * dx + dy * dy
        val r = dpF(36f)
        if (dist2 < r * r) {
            tile.alpha = 0.98f
        } else {
            tile.alpha = 1f
        }
    }

    private fun trySnap(tile: MaterialButton, letter: Char): Boolean {
        val idx = nearestFreeSlot(tile.centerX(), tile.centerY()) ?: return false
        val c = slotCenters[idx]
        val dx = c[0] - tile.centerX()
        val dy = c[1] - tile.centerY()
        val dist2 = dx * dx + dy * dy
        val snapR = dpF(44f)
        if (dist2 > snapR * snapR) return false

        // Commit placement.
        placed[idx] = letter
        slotViews[idx].text = letter.toString()
        tile.isEnabled = false

        (activity as? QuickGamesActivity)?.audio?.playClick()
        view?.let { (activity as? QuickGamesActivity)?.hapticLight(it) }

        // Animate tile into slot center.
        val tx = tile.translationX + dx
        val ty = tile.translationY + dy
        tile.animate().translationX(tx).translationY(ty).setDuration(120L).start()

        if (placed.size == target.length) {
            checkWord()
        }
        return true
    }

    private fun nearestFreeSlot(x: Float, y: Float): Int? {
        var best: Int? = null
        var bestD = Float.MAX_VALUE
        for (i in slotCenters.indices) {
            if (placed.containsKey(i)) continue
            val c = slotCenters[i]
            val dx = c[0] - x
            val dy = c[1] - y
            val d = dx * dx + dy * dy
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        return best
    }

    private fun View.centerX(): Float = translationX + width / 2f
    private fun View.centerY(): Float = translationY + height / 2f

    private fun dpF(v: Float): Float = v * resources.displayMetrics.density

    private fun shake(v: View) {
        val anim: Animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, -0.02f,
            Animation.RELATIVE_TO_SELF, 0.02f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0f
        ).apply {
            duration = 80L
            repeatMode = Animation.REVERSE
            repeatCount = 5
        }
        v.startAnimation(anim)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MINUTES = "minutes"

        fun newInstance(title: String, minutes: Int): ZenScrambleFragment {
            val f = ZenScrambleFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
