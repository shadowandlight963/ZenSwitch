package com.example.ourmajor.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.data.activities.SubActivity
import com.example.ourmajor.databinding.ItemFeaturedActivityBinding

class FeaturedActivitiesAdapter : ListAdapter<SubActivity, FeaturedActivitiesAdapter.VH>(FeaturedActivityDiffCallback()) {

    private var onItemClick: ((SubActivity) -> Unit)? = null

    fun setOnItemClick(listener: (SubActivity) -> Unit) {
        onItemClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFeaturedActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    class VH(private val binding: ItemFeaturedActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(activity: SubActivity, onItemClick: ((SubActivity) -> Unit)?) {
            binding.tvActivityTitle.text = activity.title
            binding.tvActivityDescription.text = activity.description
            binding.tvActivityDuration.text = "${activity.durationMinutes} min"
            
            // Set placeholder image for now (can be enhanced later with actual images)
            binding.ivActivityImage.setImageResource(
                when (activity.categoryId) {
                    "breathing" -> android.R.drawable.ic_menu_camera
                    "stretching" -> android.R.drawable.ic_menu_gallery
                    "mindfulness" -> android.R.drawable.ic_menu_info_details
                    "journaling" -> android.R.drawable.ic_menu_edit
                    "quick_games" -> android.R.drawable.ic_menu_more
                    else -> android.R.drawable.ic_menu_view
                }
            )
            
            // Set click listener
            binding.root.setOnClickListener {
                onItemClick?.invoke(activity)
            }
        }
    }
}

class FeaturedActivityDiffCallback : DiffUtil.ItemCallback<SubActivity>() {
    override fun areItemsTheSame(oldItem: SubActivity, newItem: SubActivity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SubActivity, newItem: SubActivity): Boolean {
        return oldItem == newItem
    }
}
