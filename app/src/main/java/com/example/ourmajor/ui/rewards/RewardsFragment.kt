package com.example.ourmajor.ui.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.example.ourmajor.R
import com.example.ourmajor.data.progress.ProgressRepository
import com.example.ourmajor.gamification.GamificationManager
import com.google.android.material.button.MaterialButton

class RewardsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, _savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rewards, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)

        // Earned badges
        view.findViewById<View>(R.id.badge_first_steps)?.setOnClickListener {
            showEarnedDialog(
                iconRes = R.drawable.ic_star_badge,
                title = "First Steps",
                tag = "milestone",
                desc = "Complete your first mindful activity",
                date = "Monday, January 15, 2024"
            )
        }
        view.findViewById<View>(R.id.badge_early_bird)?.setOnClickListener {
            showEarnedDialog(
                iconRes = R.drawable.ic_calendar,
                title = "Early Bird",
                tag = "streak",
                desc = "Complete 3 days in a row",
                date = "Wednesday, January 17, 2024"
            )
        }
        view.findViewById<View>(R.id.badge_streak_master)?.setOnClickListener {
            showEarnedDialog(
                iconRes = R.drawable.ic_bolt,
                title = "Streak Master",
                tag = "streak",
                desc = "Maintain a 7-day streak",
                date = "Sunday, January 21, 2024"
            )
        }

        // Upcoming badges
        view.findViewById<View>(R.id.badge_upcoming_activity_explorer)?.setOnClickListener {
            showUpcomingDialog(
                iconRes = R.drawable.ic_target,
                title = "Activity Explorer",
                tag = "activity",
                hint = "Try all 5 activity types"
            )
        }
        view.findViewById<View>(R.id.badge_upcoming_monthly_champion)?.setOnClickListener {
            showUpcomingDialog(
                iconRes = R.drawable.ic_calendar,
                title = "Monthly Champion",
                tag = "milestone",
                hint = "Complete activities 20 days in a month"
            )
        }
        view.findViewById<View>(R.id.badge_upcoming_zen_master)?.setOnClickListener {
            showUpcomingDialog(
                iconRes = R.drawable.ic_trophy,
                title = "Zen Master",
                tag = "milestone",
                hint = "Complete 50 activities"
            )
        }

        val totalPointsValue = view.findViewById<TextView>(R.id.total_points_value)
        val levelValue = view.findViewById<TextView>(R.id.level_value)
        val streakValue = view.findViewById<TextView>(R.id.streak_value)
        val weeklyValue = view.findViewById<TextView>(R.id.weekly_challenge_value)
        val weeklyProgress = view.findViewById<android.widget.ProgressBar>(R.id.weekly_challenge_progress)
        val weeklyHint = view.findViewById<TextView>(R.id.weekly_challenge_hint)

        val btnOcean = view.findViewById<MaterialButton>(R.id.btn_buy_ocean)
        val btnSunset = view.findViewById<MaterialButton>(R.id.btn_buy_sunset)
        val btnMinimal = view.findViewById<MaterialButton>(R.id.btn_buy_minimal)
        val btnFreeze = view.findViewById<MaterialButton>(R.id.btn_buy_freeze)

        val app = requireActivity().application
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RewardsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return RewardsViewModel(
                        application = app,
                        gamification = GamificationManager.getInstance(app.applicationContext),
                        repo = ProgressRepository()
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        val vm = ViewModelProvider(this, factory).get(RewardsViewModel::class.java)

        view.findViewById<TextView>(R.id.zen_store_title)?.setOnLongClickListener {
            vm.devUnlockAll()
            true
        }

        vm.uiState.observe(viewLifecycleOwner) { state: RewardsUiState ->
            if (!isAdded) return@observe
            runCatching {
                totalPointsValue?.text = state.totalPoints.toString()
                levelValue?.text = "Level ${state.currentLevel}"
                streakValue?.text = "Streak: ${state.currentStreak}"
                weeklyValue?.text = state.weeklyLabel
                weeklyProgress?.progress = state.weeklyProgressPercent
                weeklyHint?.text = state.weeklyHint

                if (state.oceanUnlocked) {
                    btnOcean?.text = "Unlocked"
                    btnOcean?.isEnabled = false
                    btnOcean?.alpha = 0.5f
                } else {
                    btnOcean?.text = "Unlock"
                    btnOcean?.isEnabled = true
                    btnOcean?.alpha = 1.0f
                }

                if (state.sunsetUnlocked) {
                    btnSunset?.text = "Unlocked"
                    btnSunset?.isEnabled = false
                    btnSunset?.alpha = 0.5f
                } else {
                    btnSunset?.text = "Unlock"
                    btnSunset?.isEnabled = true
                    btnSunset?.alpha = 1.0f
                }

                if (state.minimalUnlocked) {
                    btnMinimal?.text = "Unlocked"
                    btnMinimal?.isEnabled = false
                    btnMinimal?.alpha = 0.5f
                } else {
                    btnMinimal?.text = "Unlock"
                    btnMinimal?.isEnabled = true
                    btnMinimal?.alpha = 1.0f
                }

                if (state.freezeActive) {
                    btnFreeze?.text = "Active"
                    btnFreeze?.isEnabled = false
                    btnFreeze?.alpha = 0.5f
                } else {
                    btnFreeze?.text = "Unlock"
                    btnFreeze?.isEnabled = true
                    btnFreeze?.alpha = 1.0f
                }

                if (!state.lastToast.isNullOrBlank()) {
                    Toast.makeText(requireContext(), state.lastToast, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnOcean?.setOnClickListener {
            val points = vm.uiState.value?.totalPoints ?: 0
            if (points < 500) {
                Toast.makeText(requireContext(), "500 reward points needed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Unlock Ocean Breeze Theme")
                .setMessage("Spend 500 points to unlock this theme?")
                .setPositiveButton("Unlock") { _, _ -> vm.purchaseOceanTheme() }
                .setNegativeButton("Cancel", null)
                .show()
        }
        btnSunset?.setOnClickListener {
            val points = vm.uiState.value?.totalPoints ?: 0
            if (points < 500) {
                Toast.makeText(requireContext(), "500 reward points needed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Unlock Sunset Serenity Theme")
                .setMessage("Spend 500 points to unlock this theme?")
                .setPositiveButton("Unlock") { _, _ -> vm.purchaseSunsetTheme() }
                .setNegativeButton("Cancel", null)
                .show()
        }
        btnMinimal?.setOnClickListener {
            val points = vm.uiState.value?.totalPoints ?: 0
            if (points < 500) {
                Toast.makeText(requireContext(), "500 reward points needed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Unlock Minimalist Mist")
                .setMessage("Spend 500 points to unlock this theme?")
                .setPositiveButton("Unlock") { _, _ -> vm.purchaseMinimalTheme() }
                .setNegativeButton("Cancel", null)
                .show()
        }
        btnFreeze?.setOnClickListener {
            val points = vm.uiState.value?.totalPoints ?: 0
            if (points < 1000) {
                Toast.makeText(requireContext(), "1000 reward points needed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Unlock Streak Freeze")
                .setMessage("Spend 1000 points to protect your streak for 24h?")
                .setPositiveButton("Unlock") { _, _ -> vm.purchaseStreakFreeze() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showEarnedDialog(iconRes: Int, title: String, tag: String, desc: String, date: String) {
        val v = layoutInflater.inflate(R.layout.dialog_badge_earned, null)
        v.findViewById<ImageView>(R.id.icon).setImageResource(iconRes)
        v.findViewById<TextView>(R.id.title).text = title
        val tagView = v.findViewById<TextView>(R.id.tag)
        tagView.text = tag
        // Yellow for streak, green otherwise
        tagView.setBackgroundResource(if (tag.equals("streak", ignoreCase = true)) R.drawable.bg_pill_yellow else R.drawable.bg_pill_green)
        v.findViewById<TextView>(R.id.desc).text = desc
        v.findViewById<TextView>(R.id.date).text = date
        val dlg = AlertDialog.Builder(requireContext())
            .setView(v)
            .setCancelable(true)
            .create()
        dlg.show()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dlg.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        v.findViewById<View>(R.id.btn_close)?.setOnClickListener { dlg.dismiss() }
    }

    private fun showUpcomingDialog(iconRes: Int, title: String, tag: String, hint: String) {
        val v = layoutInflater.inflate(R.layout.dialog_badge_upcoming, null)
        v.findViewById<ImageView>(R.id.icon).setImageResource(iconRes)
        v.findViewById<TextView>(R.id.title).text = title
        val tagView = v.findViewById<TextView>(R.id.tag)
        tagView.text = tag
        tagView.setBackgroundResource(R.drawable.bg_pill_green)
        v.findViewById<TextView>(R.id.hint).text = hint
        val dlg = AlertDialog.Builder(requireContext())
            .setView(v)
            .setCancelable(true)
            .create()
        dlg.show()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dlg.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        v.findViewById<View>(R.id.btn_close)?.setOnClickListener { dlg.dismiss() }
    }
}
