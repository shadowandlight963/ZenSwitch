package com.example.ourmajor.ui.activities

import androidx.recyclerview.widget.DiffUtil
import com.example.ourmajor.ui.activities.ActivityItem

class ActivityItemDiffCallback : DiffUtil.ItemCallback<ActivityItem>() {
    override fun areItemsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
        return oldItem == newItem
    }
}
