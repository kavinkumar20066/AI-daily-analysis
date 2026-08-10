package com.dailyworktracker.ui.screens.summary

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

data class DailySummaryData(
    val date: LocalDate,
    val activities: List<DailyActivity> = emptyList(),
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val skippedCount: Int = 0,
    val completionRate: Float = 0f,
    val productiveHoursStr: String = "0h",
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val exerciseCount: Int = 0,
    val highlights: List<String> = emptyList(),
    val improvements: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
class DailySummaryViewModel(application: Application, dateStr: String) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository
    val date: LocalDate = try { LocalDate.parse(dateStr) } catch (_: Exception) { LocalDate.now() }

    private val _summary = MutableStateFlow(DailySummaryData(date))
    val summary: StateFlow<DailySummaryData> = _summary.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getActivitiesForDate(date).collect { activities ->
                _summary.value = computeSummary(activities)
            }
        }
    }

    private fun computeSummary(activities: List<DailyActivity>): DailySummaryData {
        val total     = activities.size
        val completed = activities.count { it.status == ActivityStatus.Completed }
        val pending   = activities.count { it.status == ActivityStatus.Pending }
        val skipped   = activities.count { it.status == ActivityStatus.Skipped }
        val rate      = if (total > 0) (completed.toFloat() / total) * 100f else 0f
        val totalMins = activities
            .filter { it.status == ActivityStatus.Completed }
            .sumOf { repo.parseDurationToMinutes(it.duration ?: "").toDouble() }
            .toInt()
        val catBreak  = activities.groupingBy { it.category }.eachCount()
        val exerciseCount = activities.count { it.isExercise }

        val highlights = buildList {
            if (completed > 0) add("✅ Completed $completed ${if (completed == 1) "activity" else "activities"}")
            if (exerciseCount > 0) add("🏃 Exercised $exerciseCount ${if (exerciseCount == 1) "time" else "times"}")
            if (totalMins > 0) add("⏱️ ${repo.formatMinutes(totalMins)} of productive time")
            val topCat = catBreak.maxByOrNull { it.value }
            if (topCat != null) add("📊 Most time in: ${topCat.key} (${topCat.value} ${if (topCat.value == 1) "activity" else "activities"})")
        }

        val improvements = buildList {
            if (rate < 50f && total > 0) add("💪 Completion rate is ${rate.toInt()}% — aim for 80%+")
            if (pending > 0) add("📋 $pending ${if (pending == 1) "activity" else "activities"} still pending")
            if (skipped > 0) add("⚠️ $skipped ${if (skipped == 1) "activity was" else "activities were"} skipped")
            if (exerciseCount == 0) add("🎯 Consider adding exercise to your routine!")
        }

        return DailySummaryData(
            date             = date,
            activities       = activities,
            totalCount       = total,
            completedCount   = completed,
            pendingCount     = pending,
            skippedCount     = skipped,
            completionRate   = rate,
            productiveHoursStr = repo.formatMinutes(totalMins),
            categoryBreakdown  = catBreak,
            exerciseCount    = exerciseCount,
            highlights       = highlights,
            improvements     = improvements
        )
    }
}
