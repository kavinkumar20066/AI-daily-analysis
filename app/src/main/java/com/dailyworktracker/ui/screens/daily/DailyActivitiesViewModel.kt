package com.dailyworktracker.ui.screens.daily

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.DailyActivity
import com.dailyworktracker.data.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class DailyActivitiesViewModel(
    application: Application,
    private val dateString: String
) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository

    val date: LocalDate = try {
        LocalDate.parse(dateString)
    } catch (e: Exception) { LocalDate.now() }

    val activities: StateFlow<List<DailyActivity>> =
        repo.getActivitiesForDate(date)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedCount: StateFlow<Int> = activities.map { list ->
        list.count { it.status == ActivityStatus.Completed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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
}
