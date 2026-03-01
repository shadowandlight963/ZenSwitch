package com.example.ourmajor.ui.journaling

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class QuickReflectionFragment : Fragment(R.layout.fragment_quick_reflection) {

    private data class Mood(val emoji: String, val label: String)

    private lateinit var cardInput: CardView
    private lateinit var cardSummary: CardView

    private lateinit var rvMood: RecyclerView
    private lateinit var chipGroupImpact: ChipGroup
    private lateinit var etNote: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private lateinit var tvMoodBig: TextView
    private lateinit var tvSummaryTitle: TextView
    private lateinit var chipGroupSummary: ChipGroup
    private lateinit var tvNoteSummary: TextView
    private lateinit var btnDone: MaterialButton

    private val moods = listOf(
        Mood("😞", "Terrible"),
        Mood("😕", "Meh"),
        Mood("🙂", "Okay"),
        Mood("😊", "Good"),
        Mood("🤩", "Amazing")
    )

    private val impacts = listOf("Sleep", "Work", "Food", "Social", "Weather")

    private var selectedMoodIndex: Int = 2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardInput = view.findViewById(R.id.cardInput)
        cardSummary = view.findViewById(R.id.cardSummary)

        rvMood = view.findViewById(R.id.rvMood)
        chipGroupImpact = view.findViewById(R.id.chipGroupImpact)
        etNote = view.findViewById(R.id.etNote)
        btnSave = view.findViewById(R.id.btnSave)

        tvMoodBig = view.findViewById(R.id.tvMoodBig)
        tvSummaryTitle = view.findViewById(R.id.tvSummaryTitle)
        chipGroupSummary = view.findViewById(R.id.chipGroupSummary)
        tvNoteSummary = view.findViewById(R.id.tvNoteSummary)
        btnDone = view.findViewById(R.id.btnDone)

        setupMoodPicker()
        setupImpactChips()

        view.doOnLayout {
            etNote.requestFocus()
            showKeyboard(etNote)
        }

        btnSave.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val note = etNote.text?.toString()?.trim().orEmpty()
            val selectedMood = moods.getOrNull(selectedMoodIndex) ?: moods[2]
            val tags = selectedImpactTags()

            hideKeyboard(etNote)
            showSummary(selectedMood, tags, note)
        }

        btnDone.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            (activity as? JournalingSessionActivity)?.recordSessionAndShowComplete()
        }
    }

    private fun setupMoodPicker() {
        rvMood.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val adapter = MoodAdapter(moods) { idx ->
            rvMood.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectedMoodIndex = idx
        }
        rvMood.adapter = adapter
        rvMood.itemAnimator = null
        rvMood.post {
            adapter.setSelected(selectedMoodIndex)
            rvMood.scrollToPosition(selectedMoodIndex)
        }
    }

    private fun setupImpactChips() {
        chipGroupImpact.removeAllViews()
        impacts.forEach { t ->
            val chip = Chip(requireContext()).apply {
                text = t
                isCheckable = true
                isCheckedIconVisible = false
                setTextColor(0xFF3A2F2A.toInt())
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(0x00FFFFFF)
                setChipStrokeColorResource(android.R.color.transparent)
                chipStrokeWidth = 0f
                setOnCheckedChangeListener { button, checked ->
                    button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    animateChipSelection(button as Chip, checked)
                }
            }
            styleChip(chip, false)
            chipGroupImpact.addView(chip)
        }
    }

    private fun animateChipSelection(chip: Chip, checked: Boolean) {
        styleChip(chip, checked)
        chip.animate().cancel()
        chip.scaleX = 0.98f
        chip.scaleY = 0.98f
        chip.animate().scaleX(1f).scaleY(1f).setDuration(160L).start()
    }

    private fun styleChip(chip: Chip, checked: Boolean) {
        val bg = if (checked) 0xFF3A2F2A.toInt() else 0xFFFDF9F0.toInt()
        val fg = if (checked) 0xFFF7F2E9.toInt() else 0xFF3A2F2A.toInt()
        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(bg)
        chip.setTextColor(fg)
    }

    private fun selectedImpactTags(): List<String> {
        val tags = ArrayList<String>()
        for (i in 0 until chipGroupImpact.childCount) {
            val c = chipGroupImpact.getChildAt(i) as? Chip ?: continue
            if (c.isChecked) tags.add(c.text?.toString().orEmpty())
        }
        return tags
    }

    private fun showSummary(mood: Mood, tags: List<String>, note: String) {
        cardInput.visibility = View.GONE
        cardSummary.visibility = View.VISIBLE
        cardSummary.alpha = 0f
        cardSummary.animate().alpha(1f).setDuration(220L).start()

        tvMoodBig.text = mood.emoji
        tvSummaryTitle.text = "Today, you felt ${mood.label.lowercase()}."

        chipGroupSummary.removeAllViews()
        tags.forEach { t ->
            val chip = Chip(requireContext()).apply {
                text = t
                isCheckable = false
                isClickable = false
                isCheckedIconVisible = false
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFF3A2F2A.toInt())
                setTextColor(0xFFF7F2E9.toInt())
            }
            chipGroupSummary.addView(chip)
        }

        tvNoteSummary.text = note
    }

    private fun showKeyboard(v: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        v.post { imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT) }
    }

    private fun hideKeyboard(v: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
        v.clearFocus()
    }

    private class MoodAdapter(
        private val moods: List<Mood>,
        private val onPick: (Int) -> Unit
    ) : RecyclerView.Adapter<MoodAdapter.VH>() {

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val card: CardView = itemView.findViewById(R.id.card)
            val emoji: TextView = itemView.findViewById(R.id.tvEmoji)
            val label: TextView = itemView.findViewById(R.id.tvLabel)
        }

        private var selected = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_mood, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = moods[position]
            holder.emoji.text = m.emoji
            holder.label.text = m.label

            val isSelected = position == selected
            holder.card.setCardBackgroundColor(if (isSelected) 0xFF3A2F2A.toInt() else 0xFFFDF9F0.toInt())
            holder.label.setTextColor(if (isSelected) 0xFFF7F2E9.toInt() else 0xFF7A6B63.toInt())

            holder.itemView.scaleX = if (isSelected) 1.06f else 1f
            holder.itemView.scaleY = if (isSelected) 1.06f else 1f

            holder.itemView.setOnClickListener {
                val prev = selected
                selected = position
                if (prev != -1) notifyItemChanged(prev)
                notifyItemChanged(position)
                holder.itemView.animate().scaleX(1.06f).scaleY(1.06f).setDuration(160L).start()
                onPick(position)
            }
        }

        override fun getItemCount(): Int = moods.size

        fun setSelected(idx: Int) {
            val prev = selected
            selected = idx
            if (prev != -1) notifyItemChanged(prev)
            notifyItemChanged(selected)
        }
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MINUTES = "minutes"

        fun newInstance(title: String, minutes: Int): QuickReflectionFragment {
            val f = QuickReflectionFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
