package com.example.ourmajor.ui.calendar



import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.example.ourmajor.R

import java.time.LocalDate



class CalendarAdapter(

    private var days: List<LocalDate?>,

    private val onDayClick: (LocalDate) -> Unit,

    private val isSelected: (LocalDate) -> Boolean,

    private val isToday: (LocalDate) -> Boolean,

    private val hasActivity: (LocalDate) -> Boolean,

) : RecyclerView.Adapter<CalendarAdapter.DayVH>() {



    fun submit(list: List<LocalDate?>) {

        days = list

        notifyDataSetChanged()

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {

        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)

        return DayVH(v)

    }



    override fun getItemCount(): Int = days.size



    override fun onBindViewHolder(holder: DayVH, position: Int) {

        val date = days[position]

        holder.bind(date, onDayClick, isSelected, isToday, hasActivity)

    }



    class DayVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tv: TextView = itemView.findViewById(R.id.tv_day)

        private val root: View = itemView.findViewById(R.id.day_root)

        private val dot: View = itemView.findViewById(R.id.dot_today)

        private val dotActivity: View = itemView.findViewById(R.id.dot_activity)

        fun bind(

            date: LocalDate?,

            onDayClick: (LocalDate) -> Unit,

            isSelected: (LocalDate) -> Boolean,

            isToday: (LocalDate) -> Boolean,

            hasActivity: (LocalDate) -> Boolean,

        ) {

            if (date == null) {

                tv.text = ""

                root.isClickable = false

                root.background = null

                dot.visibility = View.GONE

                dotActivity.visibility = View.GONE

                return

            }

            tv.text = date.dayOfMonth.toString()

            root.isClickable = true

            root.setOnClickListener { onDayClick(date) }

            val activity = hasActivity(date)

            when {

                isSelected(date) -> {

                    root.setBackgroundResource(R.drawable.bg_day_selected)

                    dot.visibility = View.GONE

                    dotActivity.visibility = View.GONE

                }

                isToday(date) -> {

                    root.setBackgroundResource(R.drawable.bg_day_today_outline)

                    dot.visibility = View.VISIBLE

                    dotActivity.visibility = if (activity) View.VISIBLE else View.GONE

                }

                else -> {

                    root.setBackgroundResource(R.drawable.bg_day_default)

                    dot.visibility = View.GONE

                    dotActivity.visibility = if (activity) View.VISIBLE else View.GONE

                }

            }

        }

    }

}