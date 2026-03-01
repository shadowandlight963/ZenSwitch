package com.example.ourmajor.ui.profile

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.R
import com.google.android.material.card.MaterialCardView
import android.widget.ImageView
import android.widget.TextView

class FocusTarotAdapter(
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<FocusTarotAdapter.VH>() {

    data class FocusItem(
        val goal: String,
        val subtitle: String,
        val iconRes: Int,
        val auraColorRes: Int
    )

    private val items = listOf(
        FocusItem("Sleep", "Deeper rest", R.drawable.ic_volume_off, R.color.zen_indigo),
        FocusItem("Energy", "Fresh spark", R.drawable.ic_bolt, R.color.zen_sunset_orange),
        FocusItem("Fun", "Light mood", R.drawable.ic_lock, R.color.zen_ocean_teal),
        FocusItem("Focus", "Deep work", R.drawable.ic_target, R.color.zen_focus_red)
    )

    private var selectedGoal: String = ""

    fun setSelectedGoal(goal: String) {
        val normalized = goal.trim()
        if (selectedGoal == normalized) return
        selectedGoal = normalized
        notifyDataSetChanged()
    }

    fun indexOfGoal(goal: String): Int {
        val normalized = goal.trim()
        return items.indexOfFirst { it.goal.equals(normalized, ignoreCase = true) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_focus_tarot, parent, false)
        return VH(v as MaterialCardView)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], items[position].goal.equals(selectedGoal, ignoreCase = true), onSelect)
    }

    class VH(private val card: MaterialCardView) : RecyclerView.ViewHolder(card) {
        private val icon: ImageView = card.findViewById(R.id.icon)
        private val title: TextView = card.findViewById(R.id.title)
        private val subtitle: TextView = card.findViewById(R.id.subtitle)

        fun bind(item: FocusItem, selected: Boolean, onSelect: (String) -> Unit) {
            title.text = item.goal
            subtitle.text = item.subtitle
            icon.setImageResource(item.iconRes)

            val ctx = card.context
            val accent = ContextCompat.getColor(ctx, item.auraColorRes)

            val scale = if (selected) 1.1f else 1.0f
            card.animate().scaleX(scale).scaleY(scale).setDuration(180).start()

            card.strokeWidth = ((ctx.resources.displayMetrics.density) * if (selected) 2f else 1f).toInt()
            card.strokeColor = if (selected) accent else 0x33FFFFFF.toInt()

            card.cardElevation = (ctx.resources.displayMetrics.density * if (selected) 10f else 2f)

            if (Build.VERSION.SDK_INT >= 28) {
                card.outlineAmbientShadowColor = if (selected) accent else 0
                card.outlineSpotShadowColor = if (selected) accent else 0
            }

            card.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onSelect(item.goal)
            }
        }
    }
}
