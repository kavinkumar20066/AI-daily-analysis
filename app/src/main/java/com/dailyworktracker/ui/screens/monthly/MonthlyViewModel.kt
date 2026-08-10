package com.dailyworktracker.ui.screens.monthly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MonthlyData(
    val yearMonth: String = "",
    val totalActivities: Int = 0,
    val completedActivities: Int = 0,
    val completionRate: Float = 0f,
    val productiveHours: Float = 0f,
    val productiveDays: Int = 0,
    val avgActivitiesPerDay: Float = 0f,
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val dailyCounts: List<Pair<String, Pair<Int,Int>>> = emptyList(), // date -> (total, completed)
    val exerciseDays: Int = 0
)

class MonthlyViewModel(application: Application, val yearMonth: String) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository

    private val _data = MutableStateFlow(MonthlyData(yearMonth))
    val data: StateFlow<MonthlyData> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getActivitiesForMonth(yearMonth).collect { all ->
                val total     = all.size
                val completed = all.count { it.status == ActivityStatus.Completed }
                val rate      = if (total > 0) completed.toFloat() / total * 100f else 0f
                val prodHours = all.filter { it.status == ActivityStatus.Completed }
                    .sumOf { repo.parseDurationToMinutes(it.duration ?: "").toDouble() }.toFloat() / 60f
                val grouped   = all.groupBy { it.date }
                val prodDays  = grouped.values.count { days ->
                    days.any { it.status == ActivityStatus.Completed }
                }
                val exDays    = grouped.values.count { days -> days.any { it.isExercise } }
                val catBreak  = all.groupingBy { it.category }.eachCount()
                val daily     = grouped.entries
                    .sortedBy { it.key }
                    .map { (date, acts) ->
                        date.toString() to (acts.size to acts.count { it.status == ActivityStatus.Completed })
                    }
                val avgPerDay = if (grouped.isNotEmpty()) total.toFloat() / grouped.size else 0f

                _data.value = MonthlyData(
                    yearMonth           = yearMonth,
                    totalActivities     = total,
                    completedActivities = completed,
                    completionRate      = rate,
                    productiveHours     = prodHours,
                    productiveDays      = prodDays,
                    avgActivitiesPerDay = avgPerDay,
                    categoryBreakdown   = catBreak,
                    dailyCounts         = daily,
                    exerciseDays        = exDays
                )
            }
        }
    }
}
