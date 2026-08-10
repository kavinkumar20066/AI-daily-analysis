package com.dailyworktracker.ui.screens.weekly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.DailyActivity
import com.dailyworktracker.data.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class WeekDayData(
    val date: LocalDate,
    val activities: List<DailyActivity>,
    val completionRate: Float,
    val productiveMinutes: Int,
    val exerciseMinutes: Int
)

data class WeeklyData(
    val weekDays: List<WeekDayData> = emptyList(),
    val totalActivities: Int = 0,
    val totalCompleted: Int = 0,
    val weeklyRate: Float = 0f,
    val totalProductiveHours: Float = 0f,
    val totalExerciseSessions: Int = 0
)

class WeeklyViewModel(application: Application, startDateStr: String) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository

    val startDate: LocalDate = try {
        LocalDate.parse(startDateStr)
    } catch (_: Exception) {
        LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
    }
    val endDate: LocalDate = startDate.plusDays(6)

    private val _weeklyData = MutableStateFlow(WeeklyData())
    val weeklyData: StateFlow<WeeklyData> = _weeklyData.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getActivitiesForDateRange(startDate, endDate).collect { all ->
                val grouped = all.groupBy { it.date }
                val weekDays = (0..6).map { offset ->
                    val day  = startDate.plusDays(offset.toLong())
                    val acts = grouped[day] ?: emptyList()
                    val completed    = acts.count { it.status == ActivityStatus.Completed }
                    val rate         = if (acts.isEmpty()) 0f else completed.toFloat() / acts.size * 100f
                    val prodMins     = acts.filter { it.status == ActivityStatus.Completed }
                        .sumOf { repo.parseDurationToMinutes(it.duration ?: "").toDouble() }.toInt()
                    val exerciseMins = acts.filter { it.isExercise && it.status == ActivityStatus.Completed }
                        .sumOf { repo.parseDurationToMinutes(it.duration ?: "").toDouble() }.toInt()
                    WeekDayData(day, acts, rate, prodMins, exerciseMins)
                }
                val total    = all.size
                val totalCom = all.count { it.status == ActivityStatus.Completed }
                _weeklyData.value = WeeklyData(
                    weekDays = weekDays,
                    totalActivities = total,
                    totalCompleted  = totalCom,
                    weeklyRate      = if (total > 0) totalCom.toFloat() / total * 100f else 0f,
                    totalProductiveHours = weekDays.sumOf { it.productiveMinutes.toDouble() }.toFloat() / 60f,
                    totalExerciseSessions = all.count { it.isExercise }
                )
            }
        }
    }
}
