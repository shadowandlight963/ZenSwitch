package com.example.ourmajor.ui.calendar



import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.example.ourmajor.R
import com.google.android.material.color.MaterialColors



data class WeekItem(

    val label: String,

    val count: Int,

    val minutes: Int,

    val progress: Float, // 0f..1f

    val isToday: Boolean

)



class WeekAdapter(private var items: List<WeekItem>) : RecyclerView.Adapter<WeekAdapter.VH>() {



    fun submit(newItems: List<WeekItem>) {

        items = newItems

        notifyDataSetChanged()

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {

        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_week_day, parent, false)

        return VH(v)

    }



    override fun onBindViewHolder(holder: VH, position: Int) {

        holder.bind(items[position])

    }



    override fun getItemCount(): Int = items.size



    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvLabel: TextView = itemView.findViewById(R.id.tv_label)

        private val dot: View = itemView.findViewById(R.id.bar_container)



        fun bind(it: WeekItem) {

            tvLabel.text = it.label

            dot.setBackgroundResource(R.drawable.bg_week_day)

            if (it.isToday) {
                tvLabel.setBackgroundResource(R.drawable.bg_day_selected)
                tvLabel.setTextColor(MaterialColors.getColor(tvLabel, com.google.android.material.R.attr.colorOnPrimary))
            } else {
                tvLabel.background = null
                tvLabel.setTextColor(MaterialColors.getColor(tvLabel, com.google.android.material.R.attr.colorOnSurfaceVariant))
            }

            val hasActivity = it.count > 0 || it.minutes > 0
            dot.alpha = if (hasActivity || it.isToday) 1f else 0.25f

        }

    }

}

