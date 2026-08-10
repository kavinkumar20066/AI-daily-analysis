package com.dailyworktracker.ui.screens.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.data.db.DailyCount
import com.dailyworktracker.data.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class CalendarDay(
    val date: LocalDate,
    val total: Int,
    val completed: Int,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
) {
    val completionRate: Float = if (total > 0) completed.toFloat() / total else -1f
    // -1 = no data, 0–0.39 = low, 0.4–0.79 = medium, ≥0.8 = high
    enum class ProductivityLevel { NONE, LOW, MEDIUM, HIGH }
    val productivityLevel: ProductivityLevel = when {
        total == 0       -> ProductivityLevel.NONE
        completionRate >= 0.8f -> ProductivityLevel.HIGH
        completionRate >= 0.4f -> ProductivityLevel.MEDIUM
        else             -> ProductivityLevel.LOW
    }
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _calendarDays = MutableStateFlow<List<CalendarDay>>(emptyList())
    val calendarDays: StateFlow<List<CalendarDay>> = _calendarDays.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    init { loadMonth(YearMonth.now()) }

    fun previousMonth() {
        val prev = _currentMonth.value.minusMonths(1)
        _currentMonth.value = prev
        loadMonth(prev)
    }

    fun nextMonth() {
        val next = _currentMonth.value.plusMonths(1)
        _currentMonth.value = next
        loadMonth(next)
    }

    fun onDaySelected(date: LocalDate) { _selectedDate.value = date }

    private fun loadMonth(ym: YearMonth) {
        viewModelScope.launch {
            val yearMonth = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val dailyCounts = repo.getDailyCountsForMonth(yearMonth)
            val countMap = dailyCounts.associateBy { it.date }

            val today = LocalDate.now()
            val firstDayOfMonth = ym.atDay(1)
            // Start grid from Monday before (or on) the 1st
            val startDay = firstDayOfMonth.minusDays(
                ((firstDayOfMonth.dayOfWeek.value - 1).toLong())
            )

            val days = mutableListOf<CalendarDay>()
            var current = startDay
            // 6 rows × 7 cols = 42 cells
            repeat(42) {
                val countEntry = countMap[current.toString()]
                days.add(CalendarDay(
                    date            = current,
                    total           = countEntry?.total ?: 0,
                    completed       = countEntry?.completed ?: 0,
                    isToday         = current == today,
                    isCurrentMonth  = current.month == ym.month
                ))
                current = current.plusDays(1)
            }
            _calendarDays.value = days
        }
    }
}
