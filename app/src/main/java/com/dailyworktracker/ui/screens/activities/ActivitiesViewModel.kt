package com.dailyworktracker.ui.screens.activities

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

/**
 * ViewModel for the Activities list screen (bottom nav tab).
 * Shows all activities for a selected date, with add/edit/delete support.
 */
class ActivitiesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val activities: StateFlow<List<DailyActivity>> = _selectedDate
        .flatMapLatest { date -> repo.getActivitiesForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount:     StateFlow<Int> = activities.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val completedCount: StateFlow<Int> = activities.map { list ->
        list.count { it.status == ActivityStatus.Completed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _pendingDelete = MutableStateFlow<DailyActivity?>(null)
    val pendingDelete: StateFlow<DailyActivity?> = _pendingDelete.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onDateChange(date: LocalDate) { _selectedDate.value = date }

    fun requestDelete(activity: DailyActivity) { _pendingDelete.value = activity }
    fun cancelDelete()                          { _pendingDelete.value = null }

    fun confirmDelete() {
        val activity = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch {
            val result = repo.deleteActivity(activity)
            if (result.isError) _errorMessage.value = result.errorMessage()
        }
    }

    fun toggleStatus(activity: DailyActivity, newStatus: ActivityStatus) {
        viewModelScope.launch {
            repo.updateActivityStatus(activity, newStatus)
        }
    }

    fun clearError() { _errorMessage.value = null }
}
