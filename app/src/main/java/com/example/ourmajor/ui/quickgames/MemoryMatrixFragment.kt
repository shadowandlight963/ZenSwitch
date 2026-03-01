package com.example.ourmajor.ui.quickgames

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.GridLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ourmajor.R
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

class MemoryMatrixFragment : Fragment(R.layout.fragment_memory_matrix) {

    private lateinit var tvStatus: TextView
    private lateinit var tvLevel: TextView
    private lateinit var grid: GridLayout

    private val ui = Handler(Looper.getMainLooper())

    private val tiles = ArrayList<MaterialButton>()

    private var level: Int = 1
    private var sequenceLen: Int = 3
    private var sequence: List<Int> = emptyList()
    private var inputIndex: Int = 0
    private var acceptingInput: Boolean = false

    private val neonCyan = Color.parseColor("#00FFFF")
    private val neonMagenta = Color.parseColor("#FF00FF")
    private val neonOff = Color.parseColor("#3A3A3A")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvStatus = view.findViewById(R.id.tvStatus)
        tvLevel = view.findViewById(R.id.tvLevel)
        grid = view.findViewById(R.id.grid)

        setupGrid()
        startRound(newLevel = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ui.removeCallbacksAndMessages(null)
    }

    private fun setupGrid() {
        grid.removeAllViews()
        grid.rowCount = 3
        grid.columnCount = 3

        val gap = dp(10)

        for (i in 0 until 9) {
            val btn = layoutInflater.inflate(R.layout.item_memory_tile, grid, false) as MaterialButton
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(i % 3, 1, 1f)
                rowSpec = GridLayout.spec(i / 3, 1, 1f)
                setMargins(gap, gap, gap, gap)
            }
            btn.layoutParams = lp
            btn.isEnabled = false
            btn.setOnClickListener { onTileTap(i) }
            setTileGlow(i, false)
            tiles.add(btn)
            grid.addView(btn)
        }
    }

    private fun startRound(newLevel: Boolean) {
        acceptingInput = false
        inputIndex = 0

        if (newLevel) {
            tvLevel.text = "Level $level"
        }

        tvStatus.text = "Watch the sequence"
        disableAll()

        sequence = buildSequence(sequenceLen)

        var t = 450L
        sequence.forEachIndexed { idx, tileIndex ->
            ui.postDelayed({ playFlash(tileIndex) }, t)
            t += if (idx == sequence.lastIndex) 520L else 420L
        }

        ui.postDelayed({
            tvStatus.text = "Your turn"
            enableAll()
            acceptingInput = true
        }, t)
    }

    private fun buildSequence(len: Int): List<Int> {
        val r = Random(System.nanoTime())
        val list = ArrayList<Int>(len)
        repeat(len) { list.add(r.nextInt(9)) }
        return list
    }

    private fun onTileTap(index: Int) {
        if (!acceptingInput) return

        val expected = sequence.getOrNull(inputIndex) ?: return
        if (index != expected) {
            acceptingInput = false
            tvStatus.text = "Try Again"
            (activity as? QuickGamesActivity)?.audio?.playDing()
            shake(grid)
            ui.postDelayed({ startRound(newLevel = false) }, 700L)
            return
        }

        view?.let { (activity as? QuickGamesActivity)?.hapticLight(it) }
        playTap(index)

        inputIndex++
        if (inputIndex >= sequence.size) {
            acceptingInput = false
            tvStatus.text = "✓"

            (activity as? QuickGamesActivity)?.audio?.playDing()
            rippleWin()

            level++
            sequenceLen++

            if (sequenceLen >= 7) {
                (activity as? QuickGamesActivity)?.recordSessionAndShowComplete(
                    title = "Memory locked",
                    subtitle = "Nice focus."
                )
                disableAll()
                return
            }

            ui.postDelayed({ startRound(newLevel = true) }, 900L)
        }
    }

    private fun playFlash(index: Int) {
        (activity as? QuickGamesActivity)?.audio?.playTileNote(index)
        neonFlash(index, isTap = false)
    }

    private fun playTap(index: Int) {
        (activity as? QuickGamesActivity)?.audio?.playTileNote(index)
        neonFlash(index, isTap = true)
    }

    private fun setTileGlow(index: Int, on: Boolean) {
        val t = tiles.getOrNull(index) ?: return
        val c = if (on) {
            if (index % 2 == 0) neonCyan else neonMagenta
        } else {
            neonOff
        }
        t.backgroundTintList = ColorStateList.valueOf(c)
        t.alpha = if (on) 1f else 0.72f
    }

    private fun neonFlash(index: Int, isTap: Boolean) {
        val v = tiles.getOrNull(index) ?: return
        val holdMs = if (isTap) 120L else 160L

        v.animate().cancel()
        v.scaleX = 1.1f
        v.scaleY = 1.1f
        setTileGlow(index, true)

        ui.postDelayed({
            v.animate().scaleX(1f).scaleY(1f).setDuration(140L).start()
            v.animate().alpha(0.72f).setDuration(180L).start()
            ui.postDelayed({ setTileGlow(index, false) }, 120L)
        }, holdMs)
    }

    private fun rippleWin() {
        disableAll()
        val order = listOf(4, 1, 3, 5, 7, 0, 2, 6, 8)
        var t = 0L
        order.forEach { idx ->
            ui.postDelayed({
                setTileGlow(idx, true)
                pulse(tiles.getOrNull(idx))
            }, t)
            ui.postDelayed({ setTileGlow(idx, false) }, t + 220L)
            t += 70L
        }
        ui.postDelayed({
            view?.let { (activity as? QuickGamesActivity)?.hapticHeavy(it) }
        }, 520L)
    }

    private fun pulse(v: View?) {
        v ?: return
        v.animate().cancel()
        v.scaleX = 1f
        v.scaleY = 1f
        v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120L).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(160L).start()
        }.start()
    }

    private fun enableAll() {
        tiles.forEach { it.isEnabled = true }
    }

    private fun disableAll() {
        tiles.forEach { it.isEnabled = false }
    }

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

        fun newInstance(title: String, minutes: Int): MemoryMatrixFragment {
            val f = MemoryMatrixFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
