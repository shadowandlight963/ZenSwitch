package com.example.ourmajor.gamification

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ourmajor.R

object RewardDialogs {

    fun showRewardSequence(activity: AppCompatActivity, result: RewardResult) {
        showRewardBreakdownDialog(activity, result) {
            if (result.isLevelUp) {
                showLevelUpDialog(activity, result.newLevel) {
                    showBadgesSequentially(activity, result.newBadges, 0)
                }
            } else {
                showBadgesSequentially(activity, result.newBadges, 0)
            }
        }
    }

    private fun showBadgesSequentially(activity: AppCompatActivity, badges: List<BadgeId>, index: Int) {
        if (index !in badges.indices) return
        showBadgeRevealDialog(activity, badges[index]) {
            showBadgesSequentially(activity, badges, index + 1)
        }
    }

    fun showRewardBreakdownDialog(
        activity: AppCompatActivity,
        result: RewardResult,
        onDismiss: () -> Unit
    ) {
        val wrap = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18))
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(activity, 20).toFloat()
                setColor(Color.parseColor("#101010"))
                setStroke(dp(activity, 1), Color.parseColor("#2AFFFFFF"))
            }
        }

        val title = TextView(activity).apply {
            text = "Rewards"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 0, 0, dp(activity, 10))
        }

        val base = TextView(activity).apply {
            text = "Base Points: ${result.basePoints}"
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 14f
        }

        val mult = TextView(activity).apply {
            text = if (result.multipliers.isEmpty()) {
                "No bonuses"
            } else {
                result.multipliers.joinToString("  •  ")
            }
            setTextColor(Color.parseColor("#88FFFFFF"))
            textSize = 13f
            setPadding(0, dp(activity, 6), 0, 0)
        }

        val total = TextView(activity).apply {
            text = "Total: 0 pts"
            setTextColor(Color.parseColor("#00FFFF"))
            textSize = 22f
            setPadding(0, dp(activity, 14), 0, 0)
        }

        wrap.addView(title)
        wrap.addView(base)
        wrap.addView(mult)
        wrap.addView(total)

        val dlg = AlertDialog.Builder(activity)
            .setView(wrap)
            .setCancelable(true)
            .create()

        dlg.setOnDismissListener { onDismiss() }
        dlg.show()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Animate count-up for earned points
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
        val anim = ValueAnimator.ofInt(0, result.pointsEarned.coerceAtLeast(0)).apply {
            duration = 650L
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val v = a.animatedValue as Int
                total.text = "Total: $v pts"
            }
        }

        // Step timing: base -> multipliers -> total count
        base.alpha = 0f
        mult.alpha = 0f
        total.alpha = 0f

        base.animate().alpha(1f).setDuration(160L).withEndAction {
            mult.animate().alpha(1f).setDuration(160L).withEndAction {
                total.animate().alpha(1f).setDuration(140L).withEndAction {
                    try {
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                    } catch (_: Exception) {
                    }
                    anim.start()
                }.start()
            }.start()
        }.start()
    }

    fun showLevelUpDialog(activity: AppCompatActivity, newLevel: Int, onDismiss: () -> Unit) {
        val root = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20))
        }

        val card = FrameLayout(activity).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(activity, 22).toFloat()
                setColor(Color.parseColor("#121212"))
                setStroke(dp(activity, 1), Color.parseColor("#44FFD666"))
            }
        }

        val confetti = ConfettiView(activity).apply {
            visibility = View.VISIBLE
        }

        val title = TextView(activity).apply {
            text = "Level $newLevel Reached!"
            setTextColor(Color.parseColor("#FFD666"))
            textSize = 22f
            gravity = Gravity.CENTER
        }

        val icon = ImageView(activity).apply {
            setImageResource(R.drawable.ic_trophy)
            setColorFilter(Color.parseColor("#FFD666"))
        }

        val inner = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(activity, 22), dp(activity, 22), dp(activity, 22), dp(activity, 22))
        }

        val iconLp = LinearLayout.LayoutParams(dp(activity, 56), dp(activity, 56)).apply {
            bottomMargin = dp(activity, 10)
        }
        inner.addView(icon, iconLp)
        inner.addView(title)

        card.addView(confetti, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        card.addView(inner, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        root.addView(card, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        val dlg = AlertDialog.Builder(activity)
            .setView(root)
            .setCancelable(true)
            .create()
        dlg.setOnDismissListener { onDismiss() }
        dlg.show()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Burst confetti + sound
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                .startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        } catch (_: Exception) {
        }
        root.post { confetti.burst() }
    }

    fun showBadgeRevealDialog(activity: AppCompatActivity, badge: BadgeId, onDismiss: () -> Unit) {
        val root = FrameLayout(activity).apply {
            setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20))
        }

        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(activity, 22).toFloat()
                setColor(Color.parseColor("#101010"))
                setStroke(dp(activity, 1), Color.parseColor("#2AFFFFFF"))
            }
            setPadding(dp(activity, 22), dp(activity, 22), dp(activity, 22), dp(activity, 22))
        }

        val icon = ImageView(activity).apply {
            setImageResource(R.drawable.ic_lock)
            setColorFilter(Color.parseColor("#88FFFFFF"))
            scaleX = 1f
            scaleY = 1f
        }

        val title = TextView(activity).apply {
            text = "Mystery Box"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, dp(activity, 10), 0, 0)
        }

        val hint = TextView(activity).apply {
            text = "Tap to reveal"
            setTextColor(Color.parseColor("#88FFFFFF"))
            textSize = 13f
            setPadding(0, dp(activity, 6), 0, 0)
        }

        card.addView(icon, LinearLayout.LayoutParams(dp(activity, 64), dp(activity, 64)))
        card.addView(title)
        card.addView(hint)

        root.addView(card, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        val dlg = AlertDialog.Builder(activity)
            .setView(root)
            .setCancelable(true)
            .create()
        dlg.setOnDismissListener { onDismiss() }
        dlg.show()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun reveal() {
            val badgeRes = when (badge) {
                BadgeId.EARLY_BIRD -> R.drawable.ic_calendar
                BadgeId.STREAK_MASTER -> R.drawable.ic_bolt
                BadgeId.ZEN_WARRIOR -> R.drawable.ic_trophy
                BadgeId.FOCUS_NINJA -> R.drawable.ic_target
            }
            title.text = badge.title
            hint.text = "Unlocked!"

            icon.animate().scaleX(0.6f).scaleY(0.6f).setDuration(140L).withEndAction {
                icon.setImageResource(badgeRes)
                icon.setColorFilter(Color.parseColor("#00FFFF"))
                icon.animate().scaleX(1.15f).scaleY(1.15f).setDuration(170L).withEndAction {
                    icon.animate().scaleX(1f).scaleY(1f).setDuration(140L).start()
                }.start()
            }.start()

            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
                    .startTone(ToneGenerator.TONE_PROP_BEEP, 70)
            } catch (_: Exception) {
            }
        }

        card.setOnClickListener { reveal() }
        // Auto reveal quickly to keep pace
        card.postDelayed({ reveal() }, 420L)
    }

    private fun dp(activity: AppCompatActivity, v: Int): Int = (v * activity.resources.displayMetrics.density).toInt()
}
