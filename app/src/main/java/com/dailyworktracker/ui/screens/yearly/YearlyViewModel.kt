package com.dailyworktracker.ui.screens.yearly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

data class MonthStat(val month: String, val label: String,
    val total: Int, val completed: Int, val rate: Float)

data class YearlyData(
    val year: String = "",
    val monthStats: List<MonthStat> = emptyList(),
    val totalActivities: Int = 0,
    val totalCompleted: Int = 0,
    val yearlyRate: Float = 0f,
    val totalProductiveHours: Float = 0f,
    val mostProductiveMonth: String = "-",
    val mostProductiveRate: Float = 0f
)

class YearlyViewModel(application: Application, val year: String) : AndroidViewModel(application) {
    private val repo: ActivityRepository = application.appContainer.activityRepository
    private val _data = MutableStateFlow(YearlyData(year))
    val data: StateFlow<YearlyData> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getActivitiesForYear(year).collect { all ->
                val grouped = all.groupBy {
                    it.date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                }
                val monthStats = (1..12).map { m ->
                    val ym     = "%s-%02d".format(year, m)
                    val label  = java.time.YearMonth.of(year.toInt(), m)
                        .format(DateTimeFormatter.ofPattern("MMM"))
                    val acts   = grouped[ym] ?: emptyList()
                    val comp   = acts.count { it.status == ActivityStatus.Completed }
                    val rate   = if (acts.isEmpty()) 0f else comp.toFloat() / acts.size * 100f
                    MonthStat(ym, label, acts.size, comp, rate)
                }
                val total    = all.size
                val completed = all.count { it.status == ActivityStatus.Completed }
                val prodHours = all.filter { it.status == ActivityStatus.Completed }
                    .sumOf { repo.parseDurationToMinutes(it.duration ?: "").toDouble() }.toFloat() / 60f
                val best     = monthStats.maxByOrNull { it.rate }

                _data.value = YearlyData(
                    year = year,
                    monthStats = monthStats,
                    totalActivities = total,
                    totalCompleted = completed,
                    yearlyRate = if (total > 0) completed.toFloat() / total * 100f else 0f,
                    totalProductiveHours = prodHours,
                    mostProductiveMonth = best?.label ?: "-",
                    mostProductiveRate = best?.rate ?: 0f
                )
            }
        }
    }
}
