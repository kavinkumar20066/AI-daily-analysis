package com.dailyworktracker.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.DailyActivity
import com.dailyworktracker.data.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchFilter(
    val query: String = "",
    val status: String = "",
    val category: String = "",
    val isExerciseOnly: Boolean = false
)

class SearchFilterViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: ActivityRepository = application.appContainer.activityRepository

    private val _filter = MutableStateFlow(SearchFilter())
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    val results: StateFlow<List<DailyActivity>> = _filter
        .debounce(300)
        .flatMapLatest { f ->
            repo.getFilteredActivities(
                query = f.query,
                status = f.status,
                category = f.category,
                isExerciseOnly = f.isExerciseOnly
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<String>> = repo.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pendingDelete = MutableStateFlow<DailyActivity?>(null)
    val pendingDelete: StateFlow<DailyActivity?> = _pendingDelete.asStateFlow()

    fun onQueryChange(q: String)               { _filter.value = _filter.value.copy(query = q) }
    fun onStatusChange(s: String)              { _filter.value = _filter.value.copy(status = s) }
    fun onCategoryChange(c: String)            { _filter.value = _filter.value.copy(category = c) }
    fun onExerciseOnlyChange(v: Boolean)       { _filter.value = _filter.value.copy(isExerciseOnly = v) }
    fun clearFilters()                         { _filter.value = SearchFilter() }

    fun requestDelete(a: DailyActivity) { _pendingDelete.value = a }
    fun cancelDelete()                  { _pendingDelete.value = null }
    fun confirmDelete() {
        val a = _pendingDelete.value ?: return; _pendingDelete.value = null
        viewModelScope.launch { repo.deleteActivity(a) }
    }

    fun toggleStatus(activity: DailyActivity, newStatus: ActivityStatus) {
        viewModelScope.launch { repo.updateActivityStatus(activity, newStatus) }
    }
}
