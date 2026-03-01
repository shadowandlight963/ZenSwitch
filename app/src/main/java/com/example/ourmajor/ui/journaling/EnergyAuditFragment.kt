package com.example.ourmajor.ui.journaling

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.ourmajor.R
import com.example.ourmajor.data.stretching.StretchingRoutines
import com.example.ourmajor.ui.breathing.BoxBreathingActivity
import com.example.ourmajor.ui.mindfulness.MindfulnessSessionActivity
import com.example.ourmajor.ui.stretching.StretchingSessionActivity
import com.google.android.material.button.MaterialButton

class EnergyAuditFragment : Fragment(R.layout.fragment_energy_audit) {

    private lateinit var sliderPhysical: BatterySliderView
    private lateinit var sliderMental: BatterySliderView
    private lateinit var sliderEmotional: BatterySliderView

    private lateinit var tvRecommendation: TextView
    private lateinit var tvRecommendationDetail: TextView
    private lateinit var btnStart: MaterialButton

    private var recommendation: Recommendation? = null

    private sealed class Recommendation {
        data class Stretching(val routineId: String) : Recommendation()
        data class BoxBreathing(val minutes: Int) : Recommendation()
        data class Gratitude(val minutes: Int) : Recommendation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sliderPhysical = view.findViewById(R.id.sliderPhysical)
        sliderMental = view.findViewById(R.id.sliderMental)
        sliderEmotional = view.findViewById(R.id.sliderEmotional)

        tvRecommendation = view.findViewById(R.id.tvRecommendation)
        tvRecommendationDetail = view.findViewById(R.id.tvRecommendationDetail)
        btnStart = view.findViewById(R.id.btnStart)

        sliderPhysical.setLevel(60)
        sliderMental.setLevel(60)
        sliderEmotional.setLevel(60)

        val listener = object : BatterySliderView.OnLevelChangedListener {
            override fun onLevelChanged(level: Int, fromUser: Boolean) {
                updateRecommendation()
            }
        }

        sliderPhysical.setOnLevelChangedListener(listener)
        sliderMental.setOnLevelChangedListener(listener)
        sliderEmotional.setOnLevelChangedListener(listener)

        updateRecommendation()

        btnStart.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val rec = recommendation ?: return@setOnClickListener

            when (rec) {
                is Recommendation.Stretching -> {
                    StretchingSessionActivity.start(requireContext(), rec.routineId)
                }
                is Recommendation.BoxBreathing -> {
                    BoxBreathingActivity.start(requireContext(), rec.minutes)
                }
                is Recommendation.Gratitude -> {
                    MindfulnessSessionActivity.start(
                        context = requireContext(),
                        mode = MindfulnessSessionActivity.Mode.GRATITUDE,
                        title = "Gratitude Moment",
                        minutes = rec.minutes
                    )
                }
            }

            (activity as? JournalingSessionActivity)?.recordSessionAndShowComplete()
        }
    }

    private fun updateRecommendation() {
        val p = sliderPhysical.getLevel()
        val m = sliderMental.getLevel()
        val e = sliderEmotional.getLevel()

        val minVal = minOf(p, m, e)
        recommendation = when (minVal) {
            p -> Recommendation.Stretching(StretchingRoutines.ROUTINE_NECK_RELIEF)
            m -> Recommendation.BoxBreathing(minutes = 2)
            else -> Recommendation.Gratitude(minutes = 2)
        }

        val label = when (recommendation) {
            is Recommendation.Stretching -> "Neck Stretching"
            is Recommendation.BoxBreathing -> "Box Breathing"
            is Recommendation.Gratitude -> "Gratitude Moment"
            null -> ""
        }

        tvRecommendation.text = "Recommendation:"
        tvRecommendationDetail.text = when (recommendation) {
            is Recommendation.Stretching -> "Your physical battery looks lowest. Try a short neck relief stretch."
            is Recommendation.BoxBreathing -> "Your mental clarity is lowest. Try a quick box breathing reset."
            is Recommendation.Gratitude -> "Your emotional balance is lowest. Try a gratitude moment to shift gently."
            null -> "Set your levels to see a suggestion."
        }

        btnStart.isEnabled = recommendation != null
        btnStart.text = if (recommendation != null) "Start Recommended Activity: $label" else "Start Recommended Activity"
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MINUTES = "minutes"

        fun newInstance(title: String, minutes: Int): EnergyAuditFragment {
            val f = EnergyAuditFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
