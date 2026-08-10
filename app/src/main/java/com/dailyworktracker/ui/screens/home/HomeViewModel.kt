package com.dailyworktracker.ui.screens.home

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

data class DashboardStats(
    val total:           Int     = 0,
    val completed:       Int     = 0,
    val pending:         Int     = 0,
    val inProgress:      Int     = 0,
    val skipped:         Int     = 0,
    val completionRate:  Float   = 0f,
    val productiveHours: Float   = 0f,
    val categoryBreakdown: Map<String, Int> = emptyMap()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository

    val today: LocalDate = LocalDate.now()

    val todayActivities: StateFlow<List<DailyActivity>> =
        repo.getActivitiesForDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<DashboardStats> = todayActivities.map { list ->
        val total     = list.size
        val completed = list.count { it.status == ActivityStatus.Completed }
        val pending   = list.count { it.status == ActivityStatus.Pending }
        val inProg    = list.count { it.status == ActivityStatus.InProgress }
        val skipped   = list.count { it.status == ActivityStatus.Skipped }
        val rate      = if (total > 0) (completed.toFloat() / total) * 100f else 0f
        val prodHours = list
            .filter { it.status == ActivityStatus.Completed }
            .sumOf { repo.parseDurationToMinutes(it.duration ?: "").toDouble() }
            .toFloat() / 60f
        val catBreakdown = list.groupingBy { it.category }.eachCount()

        DashboardStats(total, completed, pending, inProg, skipped, rate, prodHours, catBreakdown)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardStats())

    private val _pendingDelete = MutableStateFlow<DailyActivity?>(null)
    val pendingDelete: StateFlow<DailyActivity?> = _pendingDelete.asStateFlow()

    fun requestDelete(a: DailyActivity) { _pendingDelete.value = a }
    fun cancelDelete()                  { _pendingDelete.value = null }
    fun confirmDelete() {
        val a = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch { repo.deleteActivity(a) }
    }

    fun toggleStatus(activity: DailyActivity, newStatus: ActivityStatus) {
        viewModelScope.launch { repo.updateActivityStatus(activity, newStatus) }
    }

    fun greeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else      -> "Good Evening"
        }
    }

    fun formattedDate(): String =
        today.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"))
}
