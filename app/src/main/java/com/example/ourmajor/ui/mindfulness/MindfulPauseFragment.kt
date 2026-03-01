package com.example.ourmajor.ui.mindfulness

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ourmajor.R
import com.google.android.material.button.MaterialButton

class MindfulPauseFragment : Fragment(R.layout.fragment_mindful_pause) {

    private lateinit var tvStepText: TextView
    private lateinit var tvCounter: TextView
    private lateinit var btnDone: MaterialButton

    private var index: Int = 0

    private val steps = listOf(
        "Notice 5 things you see.",
        "Notice 4 things you can touch.",
        "Notice 3 things you hear.",
        "Notice 2 things you can smell.",
        "Notice 1 thing you can taste."
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvStepText = view.findViewById(R.id.tvStepText)
        tvCounter = view.findViewById(R.id.tvCounter)
        btnDone = view.findViewById(R.id.btnDone)

        bind()

        btnDone.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            index++
            if (index >= steps.size) {
                (activity as? MindfulnessSessionActivity)?.recordSessionAndShowComplete()
                return@setOnClickListener
            }
            bind()
        }
    }

    private fun bind() {
        tvStepText.text = steps[index]
        tvCounter.text = "Step ${index + 1} of ${steps.size}"
        btnDone.text = if (index == steps.size - 1) "Done" else "Next"
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MINUTES = "minutes"

        fun newInstance(title: String, minutes: Int): MindfulPauseFragment {
            val f = MindfulPauseFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
