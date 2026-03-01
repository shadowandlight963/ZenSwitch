package com.example.ourmajor.ui.journaling

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PriorityCheckFragment : Fragment(R.layout.fragment_priority_check) {

    private data class Entry(
        val text: String,
        var urgent: Boolean? = null,
        var important: Boolean? = null
    )

    private lateinit var cardInput: CardView
    private lateinit var cardPrompt: CardView
    private lateinit var cardMatrix: CardView

    private lateinit var etItem: TextInputEditText
    private lateinit var btnAdd: MaterialButton
    private lateinit var rvItems: RecyclerView
    private lateinit var btnStartSort: MaterialButton

    private lateinit var tvCurrentItem: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvStep: TextView
    private lateinit var btnYes: MaterialButton
    private lateinit var btnNo: MaterialButton

    private lateinit var grid: android.widget.GridLayout
    private lateinit var btnDone: MaterialButton

    private val entries = ArrayList<Entry>()
    private lateinit var adapter: EntryAdapter

    private var sortIndex = 0
    private var askingUrgent = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardInput = view.findViewById(R.id.cardInput)
        cardPrompt = view.findViewById(R.id.cardPrompt)
        cardMatrix = view.findViewById(R.id.cardMatrix)

        etItem = view.findViewById(R.id.etItem)
        btnAdd = view.findViewById(R.id.btnAdd)
        rvItems = view.findViewById(R.id.rvItems)
        btnStartSort = view.findViewById(R.id.btnStartSort)

        tvCurrentItem = view.findViewById(R.id.tvCurrentItem)
        tvQuestion = view.findViewById(R.id.tvQuestion)
        tvStep = view.findViewById(R.id.tvStep)
        btnYes = view.findViewById(R.id.btnYes)
        btnNo = view.findViewById(R.id.btnNo)

        grid = view.findViewById(R.id.grid)
        btnDone = view.findViewById(R.id.btnDone)

        rvItems.layoutManager = LinearLayoutManager(requireContext())
        adapter = EntryAdapter(entries)
        rvItems.adapter = adapter
        rvItems.itemAnimator = null

        btnAdd.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val t = etItem.text?.toString()?.trim().orEmpty()
            if (t.isBlank()) return@setOnClickListener
            if (entries.size >= 5) return@setOnClickListener
            entries.add(Entry(t))
            adapter.notifyItemInserted(entries.size - 1)
            etItem.setText("")
            btnStartSort.isEnabled = entries.size >= 3
        }

        btnStartSort.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            startSorting()
        }

        btnYes.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            answer(true)
        }

        btnNo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            answer(false)
        }

        btnDone.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            (activity as? JournalingSessionActivity)?.recordSessionAndShowComplete()
        }

        btnStartSort.isEnabled = entries.size >= 3
    }

    private fun startSorting() {
        if (entries.size < 3) return
        sortIndex = 0
        askingUrgent = true
        cardPrompt.visibility = View.VISIBLE
        cardPrompt.alpha = 0f
        cardPrompt.animate().alpha(1f).setDuration(220L).start()
        updatePrompt()
    }

    private fun updatePrompt() {
        val e = entries.getOrNull(sortIndex) ?: return
        tvCurrentItem.text = e.text
        tvQuestion.text = if (askingUrgent) "Is this urgent?" else "Is this important?"
        tvStep.text = "${sortIndex + 1} of ${entries.size}"
    }

    private fun answer(value: Boolean) {
        val e = entries.getOrNull(sortIndex) ?: return
        if (askingUrgent) {
            e.urgent = value
            askingUrgent = false
            updatePrompt()
            return
        }

        e.important = value
        sortIndex++
        askingUrgent = true

        if (sortIndex >= entries.size) {
            showMatrix()
        } else {
            updatePrompt()
        }
    }

    private fun showMatrix() {
        cardPrompt.visibility = View.GONE
        cardMatrix.visibility = View.VISIBLE
        cardMatrix.alpha = 0f
        cardMatrix.animate().alpha(1f).setDuration(220L).start()

        grid.removeAllViews()
        grid.columnCount = 2
        grid.rowCount = 2

        addMatrixCell(0, 0, "Do Now", entries.filter { it.urgent == true && it.important == true })
        addMatrixCell(1, 0, "Schedule", entries.filter { it.urgent == false && it.important == true })
        addMatrixCell(0, 1, "Delegate", entries.filter { it.urgent == true && it.important == false })
        addMatrixCell(1, 1, "Delete", entries.filter { it.urgent == false && it.important == false })
    }

    private fun addMatrixCell(col: Int, row: Int, title: String, list: List<Entry>) {
        val v = layoutInflater.inflate(R.layout.item_matrix_cell, grid, false)
        val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
        val container = v.findViewById<ViewGroup>(R.id.container)
        tvTitle.text = title

        list.forEach { e ->
            val tv = TextView(requireContext())
            tv.text = e.text
            tv.setTextColor(0xFF3A2F2A.toInt())
            tv.textSize = 13f
            tv.setPadding(0, dp(6), 0, dp(6))
            container.addView(tv)
        }

        val lp = android.widget.GridLayout.LayoutParams().apply {
            columnSpec = android.widget.GridLayout.spec(col, 1, 1f)
            rowSpec = android.widget.GridLayout.spec(row, 1, 1f)
            width = 0
            height = 0
        }
        grid.addView(v, lp)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private class EntryAdapter(private val items: List<Entry>) : RecyclerView.Adapter<EntryAdapter.VH>() {
        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tv: TextView = itemView.findViewById(R.id.tvText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_priority_entry, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tv.text = items[position].text
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MINUTES = "minutes"

        fun newInstance(title: String, minutes: Int): PriorityCheckFragment {
            val f = PriorityCheckFragment()
            f.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_MINUTES, minutes)
            }
            return f
        }
    }
}
