package com.example.ourmajor.ui.calendar



import androidx.lifecycle.LiveData

import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.example.ourmajor.data.history.SessionEntity

import com.example.ourmajor.data.history.SessionHistoryRepository

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.Job

import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch

import com.example.ourmajor.gamification.GamificationManager

import java.time.DayOfWeek

import java.time.LocalDate

import java.time.LocalDateTime

import java.time.YearMonth

import java.time.ZoneId

import java.time.format.DateTimeFormatter

import java.time.temporal.TemporalAdjusters



data class CalendarUiState(

    val isLoading: Boolean = true,

    val errorMessage: String? = null,

    val month: YearMonth = YearMonth.now(),

    val activeDays: Set<LocalDate> = emptySet(),

    val selectedDate: LocalDate? = LocalDate.now(),

    val currentStreakDays: Int = 0,

    val selectedDayItems: List<DaySessionItem> = emptyList(),

    val weekItems: List<WeekItem> = emptyList(),

    val summaryCount: Int = 0,

    val summaryMinutes: Int = 0,

    val summaryAvgPerDay: Float = 0f

)



class CalendarViewModel(

    private val history: SessionHistoryRepository,

    private val gm: GamificationManager

) : ViewModel() {



    private val _uiState = MutableLiveData(CalendarUiState())

    val uiState: LiveData<CalendarUiState> = _uiState



    private var monthJob: Job? = null

    private var weekJob: Job? = null



    init {

        val now = YearMonth.now()

        loadMonthlyData(now.monthValue, now.year)

        loadWeeklyStats()



        viewModelScope.launch {

            gm.state.collectLatest { st ->

                val cur = _uiState.value ?: CalendarUiState()

                _uiState.postValue(cur.copy(currentStreakDays = st.currentStreak))

            }

        }

    }



    fun loadMonthlyData(month: Int, year: Int) {

        val ym = YearMonth.of(year, month)

        val cur = _uiState.value ?: CalendarUiState()

        _uiState.postValue(cur.copy(isLoading = true, month = ym, errorMessage = null))



        monthJob?.cancel()

        val start = startOfDayMillis(ym.atDay(1))

        val end = endOfDayMillis(ym.atEndOfMonth())



        monthJob = viewModelScope.launch(Dispatchers.IO) {

            history.observeBetween(start, end).collectLatest { sessions ->

                val active = sessions

                    .map { millisToLocalDate(it.timestamp) }

                    .toSet()

                val s = _uiState.value ?: CalendarUiState()

                val selected = s.selectedDate

                val selectedItems = buildDayItems(selected, sessions)

                _uiState.postValue(

                    s.copy(

                        isLoading = false,

                        month = ym,

                        activeDays = active,

                        selectedDayItems = selectedItems,

                        errorMessage = null

                    )

                )

            }

        }

    }



    fun loadWeeklyStats() {

        val today = LocalDate.now()

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val weekEnd = weekStart.plusDays(6)



        weekJob?.cancel()

        val start = startOfDayMillis(weekStart)

        val end = endOfDayMillis(weekEnd)



        weekJob = viewModelScope.launch(Dispatchers.IO) {

            history.observeBetween(start, end).collectLatest { sessions ->

                val grouped = sessions.groupBy { millisToLocalDate(it.timestamp) }



                val counts = (0..6).map { i ->

                    val d = weekStart.plusDays(i.toLong())

                    grouped[d].orEmpty().size

                }

                val minutes = (0..6).map { i ->

                    val d = weekStart.plusDays(i.toLong())

                    grouped[d].orEmpty().sumOf {

                        ((it.durationSeconds + 59) / 60).coerceAtLeast(0)

                    }

                }



                val maxMinutes = (minutes.maxOrNull() ?: 1).coerceAtLeast(1)

                val items = (0..6).map { i ->

                    val d = weekStart.plusDays(i.toLong())

                    val mins = minutes[i]

                    val cnt = counts[i]

                    val pct = (mins.toFloat() / maxMinutes).coerceIn(0f, 1f)

                    WeekItem(

                        label = d.dayOfWeek.name.substring(0, 3).lowercase().replaceFirstChar { it.uppercase() },

                        count = cnt,

                        minutes = mins,

                        progress = pct,

                        isToday = d == today

                    )

                }



                val totalActivities = counts.sum()

                val totalMinutes = minutes.sum()

                val avgPerDay = if (items.isNotEmpty()) totalActivities.toFloat() / items.size else 0f



                val s = _uiState.value ?: CalendarUiState()

                _uiState.postValue(

                    s.copy(

                        isLoading = false,

                        weekItems = items,

                        summaryCount = totalActivities,

                        summaryMinutes = totalMinutes,

                        summaryAvgPerDay = avgPerDay,

                        errorMessage = null

                    )

                )

            }

        }

    }



    fun selectDate(date: LocalDate) {

        val s = _uiState.value ?: CalendarUiState()

        if (s.selectedDate == date) return

        _uiState.postValue(s.copy(selectedDate = date))

        // Refresh selected day items using latest known month data.

        // If month flow hasn't emitted yet, this will show empty until it does.

        viewModelScope.launch(Dispatchers.IO) {

            val current = _uiState.value ?: CalendarUiState()

            val monthStart = startOfDayMillis(current.month.atDay(1))

            val monthEnd = endOfDayMillis(current.month.atEndOfMonth())

            val sessions = history.getBetween(monthStart, monthEnd)

            val items = buildDayItems(date, sessions)

            val now = _uiState.value ?: CalendarUiState()

            _uiState.postValue(now.copy(selectedDayItems = items))

        }

    }



    fun hasActivity(date: LocalDate): Boolean {

        val active = _uiState.value?.activeDays ?: emptySet()

        return active.contains(date)

    }



    override fun onCleared() {

        super.onCleared()

        monthJob?.cancel()

        weekJob?.cancel()

    }



    private fun buildDayItems(selected: LocalDate?, sessions: List<SessionEntity>): List<DaySessionItem> {

        if (selected == null) return emptyList()

        val fmt = DateTimeFormatter.ofPattern("hh:mm a")

        val items = sessions

            .filter { millisToLocalDate(it.timestamp) == selected }

            .sortedByDescending { it.timestamp }

            .map { s ->

                val timeLabel = LocalDateTime.ofInstant(

                    java.time.Instant.ofEpochMilli(s.timestamp),

                    ZoneId.systemDefault()

                ).toLocalTime().format(fmt)

                DaySessionItem(

                    id = s.id,

                    timeLabel = timeLabel,

                    title = s.activityName,

                    subtitle = "${s.category} • ${((s.durationSeconds + 59) / 60).coerceAtLeast(1)} min",

                    pointsLabel = "+${s.pointsEarned}"

                )

            }

        return items

    }



    private fun millisToLocalDate(ms: Long): LocalDate {

        return java.time.Instant.ofEpochMilli(ms)

            .atZone(ZoneId.systemDefault())

            .toLocalDate()

    }



    private fun startOfDayMillis(date: LocalDate): Long {

        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    }



    private fun endOfDayMillis(date: LocalDate): Long {

        val next = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return next - 1L

    }

}

