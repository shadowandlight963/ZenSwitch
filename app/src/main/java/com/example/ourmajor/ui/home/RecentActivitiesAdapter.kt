package com.example.ourmajor.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.R

class RecentActivitiesAdapter : ListAdapter<RecentActivity, RecentActivitiesAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val duration: TextView = itemView.findViewById(R.id.duration)
        fun bind(item: RecentActivity) {
            val cat = item.category.lowercase()
            val (iconRes, tintRes) = when {
                cat.contains("breath") -> R.drawable.ic_nav_leaf to R.color.zen_ocean_blue
                cat.contains("stretch") -> R.drawable.ic_target to R.color.sage_primary_fixed
                cat.contains("journal") -> R.drawable.ic_edit to R.color.zen_sunset_orange
                cat.contains("game") -> R.drawable.ic_palette to R.color.zen_indigo
                else -> R.drawable.ic_star_badge to R.color.zen_lavender
            }

            icon.setImageResource(iconRes)
            icon.imageTintList = ContextCompat.getColorStateList(itemView.context, tintRes)
            title.text = item.title
            subtitle.text = item.timeAgo
            duration.text = item.duration
        }
    }

    class Diff : DiffUtil.ItemCallback<RecentActivity>() {
        override fun areItemsTheSame(oldItem: RecentActivity, newItem: RecentActivity): Boolean =
            oldItem.title == newItem.title && oldItem.timeAgo == newItem.timeAgo

        override fun areContentsTheSame(oldItem: RecentActivity, newItem: RecentActivity): Boolean =
            oldItem == newItem
    }
}
