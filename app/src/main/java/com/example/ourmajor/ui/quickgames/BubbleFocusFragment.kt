package com.example.ourmajor.ui.quickgames

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ourmajor.R

class BubbleFocusFragment : Fragment(R.layout.fragment_bubble_focus) {

    private lateinit var tank: BubbleTankView
    private lateinit var tvNext: TextView

    private var expected = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tank = view.findViewById(R.id.tank)
        tvNext = view.findViewById(R.id.tvNext)

        expected = 1
        tvNext.text = "Next: $expected"

        tank.setListener(object : BubbleTankView.Listener {
            override fun onBubbleTapped(number: Int, correct: Boolean) {
                if (correct) {
                    view.let { (activity as? QuickGamesActivity)?.hapticLight(it) }
                    (activity as? QuickGamesActivity)?.audio?.playPop()
                    expected++
                    tvNext.text = if (expected <= 10) "Next: $expected" else "Complete"
                } else {
                    (activity as? QuickGamesActivity)?.audio?.playClick()
                }
            }

            override fun onComplete() {
                (activity as? QuickGamesActivity)?.recordSessionAndShowComplete(
                    title = "Focus achieved",
                    subtitle = "Clean and steady."
                )
            }
        })

        tank.post { tank.startGame() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MINUTES = "minutes"

        fun newInstance(title: String, minutes: Int): BubbleFocusFragment {
            val f = BubbleFocusFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
