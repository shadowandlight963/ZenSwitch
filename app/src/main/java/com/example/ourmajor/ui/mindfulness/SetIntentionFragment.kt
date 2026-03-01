package com.example.ourmajor.ui.mindfulness

import android.os.Bundle
import android.os.CountDownTimer
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.R
import com.google.android.material.button.MaterialButton

class SetIntentionFragment : Fragment(R.layout.fragment_set_intention) {

    private lateinit var cardPick: CardView
    private lateinit var rvWords: RecyclerView
    private lateinit var cardFocus: CardView
    private lateinit var tvWord: TextView
    private lateinit var tvTimer: TextView

    private var timer: CountDownTimer? = null
    private var started: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardPick = view.findViewById(R.id.cardPick)
        rvWords = view.findViewById(R.id.rvWords)
        cardFocus = view.findViewById(R.id.cardFocus)
        tvWord = view.findViewById(R.id.tvWord)
        tvTimer = view.findViewById(R.id.tvTimer)

        rvWords.layoutManager = GridLayoutManager(requireContext(), 2)
        rvWords.adapter = PowerWordAdapter(
            words = listOf("Focus", "Kindness", "Strength", "Patience", "Calm"),
            onPick = { w ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                startFocus(w)
            }
        )
    }

    private fun startFocus(word: String) {
        if (started) return
        started = true

        tvWord.text = word
        cardFocus.visibility = View.VISIBLE
        cardPick.animate().alpha(0f).setDuration(200L).withEndAction {
            cardPick.visibility = View.GONE
        }.start()

        val breathe = tvWord.animate()
        breathe.setDuration(2000L)
        breathe.setInterpolator(AccelerateDecelerateInterpolator())

        tvWord.scaleX = 1f
        tvWord.scaleY = 1f

        tvWord.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(2000L)
            .withEndAction {
                tvWord.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(2000L)
                    .withEndAction {
                        if (started) startFocusBreathingLoop()
                    }
                    .start()
            }
            .start()

        timer?.cancel()
        timer = object : CountDownTimer(60_000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val s = (millisUntilFinished / 1000L).toInt().coerceAtLeast(0)
                tvTimer.text = "${s}s"
            }

            override fun onFinish() {
                tvTimer.text = "0s"
                started = false
                (activity as? MindfulnessSessionActivity)?.recordSessionAndShowComplete()
            }
        }.start()
    }

    private fun startFocusBreathingLoop() {
        if (!started) return
        tvWord.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(2000L)
            .withEndAction {
                if (!started) return@withEndAction
                tvWord.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(2000L)
                    .withEndAction { startFocusBreathingLoop() }
                    .start()
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        timer = null
        started = false
    }

    private class PowerWordAdapter(
        private val words: List<String>,
        private val onPick: (String) -> Unit
    ) : RecyclerView.Adapter<PowerWordAdapter.VH>() {

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val btn: MaterialButton = itemView.findViewById(R.id.btnWord)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_power_word, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val w = words[position]
            holder.btn.text = w
            holder.btn.setOnClickListener { onPick(w) }
        }

        override fun getItemCount(): Int = words.size
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MINUTES = "minutes"

        fun newInstance(title: String, minutes: Int): SetIntentionFragment {
            val f = SetIntentionFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
