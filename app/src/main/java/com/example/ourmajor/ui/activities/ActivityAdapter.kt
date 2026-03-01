package com.example.ourmajor.ui.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.color.MaterialColors
import com.example.ourmajor.R
import com.example.ourmajor.ui.breathing.BoxBreathingActivity
import com.example.ourmajor.ui.breathing.ResonantBreathActivity
import com.example.ourmajor.ui.breathing.SleepBreathActivity
import com.example.ourmajor.ui.stretching.StretchingSessionActivity
import com.example.ourmajor.data.stretching.StretchingRoutines
import com.example.ourmajor.ui.mindfulness.MindfulnessSessionActivity
import com.example.ourmajor.ui.journaling.JournalingSessionActivity
import com.example.ourmajor.ui.quickgames.QuickGamesActivity

data class ActivityItem(
    val title: String,
    val description: String,
    val minutes: Int,
    val category: String
)

class ActivityAdapter : ListAdapter<ActivityItem, ActivityAdapter.VH>(ActivityItemDiffCallback()) {

    private var highlightTitle: String? = null
    private var favorites: Set<String> = emptySet()
    private var onFavoriteClick: ((String, Boolean) -> Unit)? = null
    private var onItemClick: ((ActivityItem) -> Unit)? = null

    fun highlight(title: String?) {
        highlightTitle = title
        notifyDataSetChanged()
    }

    fun setFavorites(newFavorites: Set<String>) {
        favorites = newFavorites
        notifyDataSetChanged()
    }

    fun setOnFavoriteClick(listener: (String, Boolean) -> Unit) {
        onFavoriteClick = listener
    }

    fun setOnItemClick(listener: (ActivityItem) -> Unit) {
        onItemClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item, highlightTitle, item.title in favorites, onFavoriteClick, onItemClick)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
        private val tvDur: TextView = itemView.findViewById(R.id.tv_duration)
        private val btnStart: Button = itemView.findViewById(R.id.btn_start)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btn_favorite)
        private val btnTime1Min: Button = itemView.findViewById(R.id.btn_time_1min)
        private val btnTime2Min: Button = itemView.findViewById(R.id.btn_time_2min)
        private val btnTime3Min: Button = itemView.findViewById(R.id.btn_time_3min)
        private val tvCompletionStatus: TextView = itemView.findViewById(R.id.tv_completion_status)
        private val tvLastCompleted: TextView = itemView.findViewById(R.id.tv_last_completed)
        private val card: View = itemView.findViewById(R.id.card_root)

        fun bind(item: ActivityItem, highlightTitle: String?, isFavorite: Boolean, onFavoriteClick: ((String, Boolean) -> Unit)?, onItemClick: ((ActivityItem) -> Unit)?) {
            tvTitle.text = item.title
            tvDesc.text = item.description
            tvDur.text = "${item.minutes} min"
            card.setBackgroundResource(if (item.title == highlightTitle) R.drawable.bg_card_soft_highlight else R.drawable.bg_card_soft)
            card.setOnClickListener { onItemClick?.invoke(item) }

            btnFavorite.visibility = View.GONE
            btnTime1Min.visibility = View.GONE
            btnTime2Min.visibility = View.GONE
            btnTime3Min.visibility = View.GONE
            tvCompletionStatus.visibility = View.GONE
            tvLastCompleted.visibility = View.GONE
            
            btnStart.setOnClickListener {
                try {
                    val ctx = itemView.context

                    if (item.title.equals("Box Breathing", ignoreCase = true)) {
                        BoxBreathingActivity.start(ctx, item.minutes)
                    } else if (
                        item.title.equals("4-7-8 Sleep Breath", ignoreCase = true) ||
                        item.title.equals("Deep Breathing", ignoreCase = true)
                    ) {
                        SleepBreathActivity.start(ctx, item.minutes)
                    } else if (
                        item.title.equals("Resonant Wave", ignoreCase = true)
                    ) {
                        ResonantBreathActivity.start(ctx, item.minutes)
                    } else if (
                        item.title.equals("Neck Relief", ignoreCase = true) ||
                        item.title.equals("Neck Stretching", ignoreCase = true)
                    ) {
                        StretchingSessionActivity.start(ctx, StretchingRoutines.ROUTINE_NECK_RELIEF)
                    } else if (
                        item.title.equals("Spine Mobility", ignoreCase = true) ||
                        item.title.equals("Core Stretching", ignoreCase = true)
                    ) {
                        StretchingSessionActivity.start(ctx, StretchingRoutines.ROUTINE_SPINE_MOBILITY)
                    } else if (
                        item.title.equals("Full Body Unwind", ignoreCase = true) ||
                        item.title.equals("Stretching for Relaxation", ignoreCase = true)
                    ) {
                        StretchingSessionActivity.start(ctx, StretchingRoutines.ROUTINE_FULL_BODY_UNWIND)
                    } else if (item.title.equals("Gratitude Moment", ignoreCase = true)) {
                        MindfulnessSessionActivity.start(
                            context = ctx,
                            mode = MindfulnessSessionActivity.Mode.GRATITUDE,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else if (item.title.equals("Set Intention", ignoreCase = true)) {
                        MindfulnessSessionActivity.start(
                            context = ctx,
                            mode = MindfulnessSessionActivity.Mode.INTENTION,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else if (item.title.equals("Mindful Pause", ignoreCase = true)) {
                        MindfulnessSessionActivity.start(
                            context = ctx,
                            mode = MindfulnessSessionActivity.Mode.PAUSE,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else if (item.title.equals("Quick Reflection", ignoreCase = true)) {
                        JournalingSessionActivity.start(
                            context = ctx,
                            mode = JournalingSessionActivity.Mode.QUICK_REFLECTION,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else if (item.title.equals("Priority Check", ignoreCase = true)) {
                        JournalingSessionActivity.start(
                            context = ctx,
                            mode = JournalingSessionActivity.Mode.PRIORITY_CHECK,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else if (item.title.equals("Energy Audit", ignoreCase = true)) {
                        JournalingSessionActivity.start(
                            context = ctx,
                            mode = JournalingSessionActivity.Mode.ENERGY_AUDIT,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else if (item.title.equals("Memory Matrix", ignoreCase = true)) {
                        QuickGamesActivity.start(
                            context = ctx,
                            mode = QuickGamesActivity.Mode.MEMORY_MATRIX,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else if (item.title.equals("Bubble Focus", ignoreCase = true)) {
                        QuickGamesActivity.start(
                            context = ctx,
                            mode = QuickGamesActivity.Mode.BUBBLE_FOCUS,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else if (item.title.equals("Zen Scramble", ignoreCase = true)) {
                        QuickGamesActivity.start(
                            context = ctx,
                            mode = QuickGamesActivity.Mode.ZEN_SCRAMBLE,
                            title = item.title,
                            minutes = item.minutes
                        )
                    } else {
                        ExerciseActivity.start(
                            context = ctx,
                            name = item.title,
                            minutes = item.minutes,
                            breathing = item.category.equals("breathing", ignoreCase = true),
                            category = item.category
                        )
                    }
                } catch (e: Exception) {
                    // Log error but don't crash the app
                    android.util.Log.e("ActivityAdapter", "Failed to start exercise activity", e)
                }
            }
        }
        
        private fun updateTimeButtonSelection(selectedMinutes: Int) {
            // Reset all buttons to default state
            resetTimeButtons()

            val primary = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorPrimary)
            
            // Highlight selected button
            when (selectedMinutes) {
                1 -> {
                    btnTime1Min.setTextColor(primary)
                    btnTime1Min.setBackgroundColor(itemView.context.getColor(android.R.color.white))
                }
                2 -> {
                    btnTime2Min.setTextColor(primary)
                    btnTime2Min.setBackgroundColor(itemView.context.getColor(android.R.color.white))
                }
                3 -> {
                    btnTime3Min.setTextColor(primary)
                    btnTime3Min.setBackgroundColor(itemView.context.getColor(android.R.color.white))
                }
            }
        }
        
        private fun resetTimeButtons() {
            val defaultColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnSurfaceVariant)
            btnTime1Min.setTextColor(defaultColor)
            btnTime2Min.setTextColor(defaultColor)
            btnTime3Min.setTextColor(defaultColor)
        }
        
        private fun updateCompletionStatus(item: ActivityItem) {
            // For now, use placeholder completion data
            // In a real implementation, this would come from the ViewModel
            val completionCount = (0..5).random() // Placeholder random completion count
            val wasCompletedToday = (0..1).random() == 1 // Placeholder random completion today
            
            if (completionCount > 0) {
                tvCompletionStatus.text = "Completed $completionCount time${if (completionCount != 1) "s" else ""}"
                tvCompletionStatus.visibility = View.VISIBLE
                
                if (wasCompletedToday) {
                    tvLastCompleted.text = "Last: Today"
                    tvLastCompleted.visibility = View.VISIBLE
                } else {
                    tvLastCompleted.visibility = View.GONE
                }
            } else {
                tvCompletionStatus.visibility = View.GONE
                tvLastCompleted.visibility = View.GONE
            }
        }
    }
}
