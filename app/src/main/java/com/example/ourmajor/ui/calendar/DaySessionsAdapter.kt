package com.example.ourmajor.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.R

data class DaySessionItem(
    val id: String,
    val timeLabel: String,
    val title: String,
    val subtitle: String,
    val pointsLabel: String
)

class DaySessionsAdapter : ListAdapter<DaySessionItem, DaySessionsAdapter.VH>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_day_session, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_subtitle)
        private val tvPoints: TextView = itemView.findViewById(R.id.tv_points)

        fun bind(it: DaySessionItem) {
            tvTime.text = it.timeLabel
            tvTitle.text = it.title
            tvSubtitle.text = it.subtitle
            tvPoints.text = it.pointsLabel
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<DaySessionItem>() {
        override fun areItemsTheSame(oldItem: DaySessionItem, newItem: DaySessionItem): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: DaySessionItem, newItem: DaySessionItem): Boolean {
            return oldItem == newItem
        }
    }
}