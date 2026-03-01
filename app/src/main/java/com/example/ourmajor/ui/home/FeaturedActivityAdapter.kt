package com.example.ourmajor.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.databinding.ItemFeaturedActivityBinding

class FeaturedActivityAdapter : ListAdapter<FeaturedActivity, FeaturedActivityAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFeaturedActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemFeaturedActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FeaturedActivity) {
            binding.tvActivityTitle.text = item.title
            binding.tvActivityDescription.text = item.description
            binding.ivActivityImage.setImageResource(item.imageResId)

            binding.root.setOnClickListener {
                // no-op for now
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<FeaturedActivity>() {
        override fun areItemsTheSame(oldItem: FeaturedActivity, newItem: FeaturedActivity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FeaturedActivity, newItem: FeaturedActivity): Boolean =
            oldItem == newItem
    }
}
