package com.example.ourmajor.ui.calendar



import android.os.Bundle

import android.view.LayoutInflater

import android.view.View

import android.view.ViewGroup

import android.widget.ImageView

import android.widget.LinearLayout

import android.widget.TextView

import androidx.fragment.app.Fragment

import androidx.lifecycle.Lifecycle

import androidx.lifecycle.ViewModelProvider

import androidx.lifecycle.ViewModel

import androidx.lifecycle.lifecycleScope

import androidx.lifecycle.repeatOnLifecycle

import androidx.recyclerview.widget.GridLayoutManager

import androidx.recyclerview.widget.LinearLayoutManager

import androidx.recyclerview.widget.RecyclerView

import com.example.ourmajor.R

import com.example.ourmajor.data.history.SessionHistoryRepository

import com.example.ourmajor.gamification.GamificationManager

import com.google.android.material.floatingactionbutton.FloatingActionButton

import com.google.android.material.datepicker.MaterialDatePicker

import java.time.Instant

import java.time.LocalDate

import java.time.YearMonth

import java.time.ZoneId

import java.time.format.DateTimeFormatter

import kotlinx.coroutines.launch



class CalendarFragment : Fragment() {

    private lateinit var tvMonth: TextView

    private lateinit var chipStreak: TextView

    private lateinit var chipWeekActivities: TextView

    private lateinit var grid: RecyclerView

    private lateinit var adapter: CalendarAdapter

    private lateinit var weekDaysContainer: LinearLayout

    private lateinit var summaryActivities: TextView

    private lateinit var summaryAvg: TextView

    private lateinit var summaryMinutes: TextView

    private lateinit var dayTitle: TextView

    private lateinit var dayEmpty: TextView

    private lateinit var dayList: RecyclerView

    private lateinit var dayAdapter: DaySessionsAdapter

    private var currentMonth: YearMonth = YearMonth.now()

    private var selected: LocalDate? = LocalDate.now()



    private var lastActiveDays: Set<LocalDate> = emptySet()



    private lateinit var vm: CalendarViewModel



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, _savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)

        tvMonth = view.findViewById(R.id.tv_month)

        chipStreak = view.findViewById(R.id.chip_streak)

        chipWeekActivities = view.findViewById(R.id.chip_week_activities)

        grid = view.findViewById(R.id.calendarGrid)

        grid.layoutManager = GridLayoutManager(requireContext(), 7)



        val appContext = requireContext().applicationContext

        val factory = object : ViewModelProvider.Factory {

            override fun <T : ViewModel> create(modelClass: Class<T>): T {

                if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {

                    @Suppress("UNCHECKED_CAST")

                    return CalendarViewModel(

                        SessionHistoryRepository(appContext),

                        GamificationManager.getInstance(appContext)

                    ) as T

                }

                throw IllegalArgumentException("Unknown ViewModel class")

            }

        }

        vm = ViewModelProvider(this, factory).get(CalendarViewModel::class.java)



        adapter = CalendarAdapter(

            emptyList(),

            onDayClick = { date ->

                vm.selectDate(date)

            },

            isSelected = { date -> selected == date },

            isToday = { date -> date == LocalDate.now() },

            hasActivity = { date -> vm.hasActivity(date) }

        )

        grid.adapter = adapter




        // Weekly progress strip

        weekDaysContainer = view.findViewById(R.id.week_days_container)

        summaryActivities = view.findViewById(R.id.tv_summary_activities)

        summaryAvg = view.findViewById(R.id.tv_summary_avg)

        summaryMinutes = view.findViewById(R.id.tv_summary_minutes)



        // Selected day details

        dayTitle = view.findViewById(R.id.day_details_title)

        dayEmpty = view.findViewById(R.id.day_details_empty)

        dayList = view.findViewById(R.id.daySessionList)

        dayList.layoutManager = LinearLayoutManager(requireContext())

        dayAdapter = DaySessionsAdapter()

        dayList.adapter = dayAdapter



        view.findViewById<ImageView>(R.id.btn_prev)?.setOnClickListener {

            currentMonth = currentMonth.minusMonths(1)

            vm.loadMonthlyData(currentMonth.monthValue, currentMonth.year)

        }

        view.findViewById<ImageView>(R.id.btn_next)?.setOnClickListener {

            currentMonth = currentMonth.plusMonths(1)

            vm.loadMonthlyData(currentMonth.monthValue, currentMonth.year)

        }



        tvMonth.setOnClickListener {

            val selection = currentMonth.atDay(1)

                .atStartOfDay(ZoneId.systemDefault())

                .toInstant()

                .toEpochMilli()

            val picker = MaterialDatePicker.Builder.datePicker()

                .setSelection(selection)

                .build()

            picker.addOnPositiveButtonClickListener { millis ->

                val picked = Instant.ofEpochMilli(millis)

                    .atZone(ZoneId.systemDefault())

                    .toLocalDate()

                currentMonth = YearMonth.of(picked.year, picked.monthValue)

                vm.loadMonthlyData(currentMonth.monthValue, currentMonth.year)

                vm.selectDate(picked)

            }

            picker.show(parentFragmentManager, "month_picker")

        }



        renderMonth()



        vm.loadMonthlyData(currentMonth.monthValue, currentMonth.year)

        vm.loadWeeklyStats()



        vm.uiState.observe(viewLifecycleOwner) { state ->

            val selectedChanged = selected != state.selectedDate

            val monthChanged = currentMonth != state.month

            val activeDaysChanged = lastActiveDays != state.activeDays



            selected = state.selectedDate

            currentMonth = state.month

            lastActiveDays = state.activeDays




            renderWeekStrip(state.weekItems)



            val streak = state.currentStreakDays

            chipStreak.text = if (streak <= 0) "Start streak" else "$streak day streak"

            chipWeekActivities.text = "${state.summaryCount} activities this week"



            val fmt = DateTimeFormatter.ofPattern("EEE, MMM d")

            val selectedLabel = state.selectedDate?.format(fmt) ?: ""

            dayTitle.text = if (selectedLabel.isBlank()) "Selected Day" else "Selected Day • $selectedLabel"



            dayAdapter.submitList(state.selectedDayItems)

            val isEmpty = state.selectedDayItems.isEmpty()

            dayEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE

            dayList.visibility = if (isEmpty) View.GONE else View.VISIBLE



            if (selectedChanged || monthChanged || activeDaysChanged) {

                renderMonth()

            }

        }

    }



    private fun renderWeekStrip(items: List<WeekItem>) {
        var totalActivities = 0
        var totalMinutes = 0

        val fallbackLabels = arrayOf("M", "Tu", "W", "Th", "F", "Sa", "Su")
        val slots = weekDaysContainer.childCount

        for (i in 0 until slots) {
            val v = weekDaysContainer.getChildAt(i)
            val tv = v.findViewById<TextView>(R.id.tv_label)
            val dot = v.findViewById<View>(R.id.bar_container)
            val tvValue = v.findViewById<TextView>(R.id.tv_value)

            val day: WeekItem? = items.getOrNull(i)

            tv.text = day?.let { shortWeekLabel(it.label) } ?: fallbackLabels.getOrNull(i).orEmpty()
            tvValue.text = day?.let { "${it.minutes}m" } ?: "0m"

            dot.setBackgroundResource(R.drawable.bg_week_day)

            if (day?.isToday == true) {
                tv.setBackgroundResource(R.drawable.bg_day_selected)
                tv.setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        tv,
                        com.google.android.material.R.attr.colorOnPrimary
                    )
                )
            } else {
                tv.background = null
                tv.setTextColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        tv,
                        com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
            }

            val hasActivity = (day?.count ?: 0) > 0 || (day?.minutes ?: 0) > 0
            dot.alpha = if (hasActivity || day?.isToday == true) 1f else 0.25f

            totalActivities += day?.count ?: 0
            totalMinutes += day?.minutes ?: 0
        }

        val days = if (slots > 0) slots else 0
        val avgPerDay = if (days > 0) totalActivities.toFloat() / days.toFloat() else 0f

        summaryActivities.text = totalActivities.toString()
        summaryMinutes.text = totalMinutes.toString()
        summaryAvg.text = String.format("%.1f", avgPerDay)
    }



    private fun shortWeekLabel(label: String): String {
        return when (label.trim().take(3).lowercase()) {
            "mon" -> "M"
            "tue" -> "Tu"
            "wed" -> "W"
            "thu" -> "Th"
            "fri" -> "F"
            "sat" -> "Sa"
            "sun" -> "Su"
            else -> label.trim().take(2)
        }
    }



    private fun renderMonth() {

        val fmt = DateTimeFormatter.ofPattern("MMMM yyyy")

        tvMonth.text = currentMonth.atDay(1).format(fmt)

        adapter.submit(buildMonth(currentMonth))

    }



    private fun buildMonth(month: YearMonth): List<LocalDate?> {

        val first = month.atDay(1)

        val daysInMonth = month.lengthOfMonth()

        val firstDayOfWeekIndex = (first.dayOfWeek.value % 7) // Sun=0, Mon=1 ...

        val list = mutableListOf<LocalDate?>()

        repeat(firstDayOfWeekIndex) { list.add(null) }

        for (d in 1..daysInMonth) list.add(month.atDay(d))

        // pad to complete rows of 7

        while (list.size % 7 != 0) list.add(null)

        return list

    }



    // Week stats are now driven by Firestore via CalendarViewModel

}