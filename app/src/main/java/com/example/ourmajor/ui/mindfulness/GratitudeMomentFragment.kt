package com.example.ourmajor.ui.mindfulness

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ourmajor.R
import com.example.ourmajor.data.mindfulness.GratitudeRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class GratitudeMomentFragment : Fragment(R.layout.fragment_gratitude_moment) {

    private lateinit var et: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private lateinit var overlay: FrameLayout
    private lateinit var jarContainer: FrameLayout

    private val repo: GratitudeRepository by lazy { GratitudeRepository(requireContext().applicationContext) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        et = view.findViewById(R.id.etGratitude)
        btnSave = view.findViewById(R.id.btnSave)
        overlay = view.findViewById(R.id.overlay)
        jarContainer = view.findViewById(R.id.jarContainer)

        view.doOnLayout {
            et.requestFocus()
            showKeyboard(et)
        }

        btnSave.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val text = et.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener

            hideKeyboard(et)

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    repo.addEntry(text)
                }
                playFireflyAnimation(text)
            }
        }
    }

    private fun playFireflyAnimation(text: String) {
        val activity = activity as? MindfulnessSessionActivity

        val start = IntArray(2)
        et.getLocationOnScreen(start)
        val overlayLoc = IntArray(2)
        overlay.getLocationOnScreen(overlayLoc)

        val startX = start[0] - overlayLoc[0] + et.width * 0.5f
        val startY = start[1] - overlayLoc[1] + et.height * 0.5f

        val jarLoc = IntArray(2)
        jarContainer.getLocationOnScreen(jarLoc)
        val endX = jarLoc[0] - overlayLoc[0] + jarContainer.width * 0.5f
        val endY = jarLoc[1] - overlayLoc[1] + jarContainer.height * 0.5f

        val firefly = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_star_badge)
            setColorFilter(0xEEFFFFFF.toInt())
            alpha = 0f
            scaleX = 1.2f
            scaleY = 1.2f
        }

        val size = dp(26)
        val lp = FrameLayout.LayoutParams(size, size)
        overlay.addView(firefly, lp)
        firefly.translationX = startX - size / 2f
        firefly.translationY = startY - size / 2f

        firefly.animate()
            .alpha(1f)
            .setDuration(120L)
            .start()

        firefly.animate()
            .translationX(endX - size / 2f)
            .translationY(endY - size / 2f)
            .scaleX(0.55f)
            .scaleY(0.55f)
            .setDuration(650L)
            .withEndAction {
                overlay.removeView(firefly)
                dropStarIntoJar()
                et.setText("")
                activity?.recordSessionAndShowComplete()
            }
            .start()
    }

    private fun dropStarIntoJar() {
        val star = View(requireContext())
        val d = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xCCFFFFFF.toInt())
        }
        star.background = d

        val s = dp(10)
        val lp = FrameLayout.LayoutParams(s, s)
        jarContainer.addView(star, lp)

        val maxX = max(0, jarContainer.width - s)
        val maxY = max(0, jarContainer.height - s)

        star.translationX = (0..maxX).random().toFloat()
        star.translationY = (0..maxY).random().toFloat()
        star.alpha = 0f
        star.scaleX = 0.2f
        star.scaleY = 0.2f

        star.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(240L).start()
    }

    private fun showKeyboard(v: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        v.post { imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT) }
    }

    private fun hideKeyboard(v: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
        v.clearFocus()
    }

    private fun dp(v: Int): Int {
        val d = resources.displayMetrics.density
        return (v * d).toInt()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MINUTES = "minutes"

        fun newInstance(title: String, minutes: Int): GratitudeMomentFragment {
            val f = GratitudeMomentFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
