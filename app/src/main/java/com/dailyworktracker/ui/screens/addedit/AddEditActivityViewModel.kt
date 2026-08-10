package com.dailyworktracker.ui.screens.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.data.model.ActivityPriority
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.Categories
import com.dailyworktracker.data.model.DailyActivity
import com.dailyworktracker.data.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for the Add / Edit Activity screen.
 * Manages form state, validation, and delegates saves to ActivityRepository.
 */
class AddEditActivityViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository

    // null = Add mode, non-null = Edit mode
    private val activityId: String? = savedStateHandle["activityId"]

    // ─── Form Fields as StateFlow ─────────────────────────────────────────────

    private val _activityName    = MutableStateFlow("")
    private val _category        = MutableStateFlow("Other")
    private val _customCategory  = MutableStateFlow("")
    private val _date            = MutableStateFlow(LocalDate.now())
    private val _startTime       = MutableStateFlow("")
    private val _endTime         = MutableStateFlow("")
    private val _duration        = MutableStateFlow("")
    private val _status          = MutableStateFlow(ActivityStatus.Pending)
    private val _priority        = MutableStateFlow(ActivityPriority.Medium)
    private val _notes           = MutableStateFlow("")
    private val _isExercise      = MutableStateFlow(false)
    private val _exerciseType    = MutableStateFlow("Other")
    private val _distance        = MutableStateFlow("")
    private val _calories        = MutableStateFlow("")

    val activityName:   StateFlow<String>          = _activityName.asStateFlow()
    val category:       StateFlow<String>          = _category.asStateFlow()
    val customCategory: StateFlow<String>          = _customCategory.asStateFlow()
    val date:           StateFlow<LocalDate>       = _date.asStateFlow()
    val startTime:      StateFlow<String>          = _startTime.asStateFlow()
    val endTime:        StateFlow<String>          = _endTime.asStateFlow()
    val duration:       StateFlow<String>          = _duration.asStateFlow()
    val status:         StateFlow<ActivityStatus>  = _status.asStateFlow()
    val priority:       StateFlow<ActivityPriority>= _priority.asStateFlow()
    val notes:          StateFlow<String>          = _notes.asStateFlow()
    val isExercise:     StateFlow<Boolean>         = _isExercise.asStateFlow()
    val exerciseType:   StateFlow<String>          = _exerciseType.asStateFlow()
    val distance:       StateFlow<String>          = _distance.asStateFlow()
    val calories:       StateFlow<String>          = _calories.asStateFlow()

    // ─── UI State ─────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<AddEditUiState>(AddEditUiState.Idle)
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    val isEditMode: Boolean get() = activityId != null

    private var originalActivity: DailyActivity? = null

    // Validation errors
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    init {
        if (activityId != null) {
            loadActivityForEdit(activityId)
        }
    }

    private fun loadActivityForEdit(id: String) {
        viewModelScope.launch {
            _uiState.value = AddEditUiState.Loading
            val activity = repo.getActivityById(id)
            if (activity == null) {
                _uiState.value = AddEditUiState.Error("Activity not found")
                return@launch
            }
            originalActivity = activity
            populateFields(activity)
            _uiState.value = AddEditUiState.Idle
        }
    }

    private fun populateFields(a: DailyActivity) {
        _activityName.value = a.activityName
        // Check if category is in predefined list
        if (a.category in Categories.predefined) {
            _category.value = a.category
        } else {
            _category.value = "Custom"
            _customCategory.value = a.category
        }
        _date.value      = a.date
        _startTime.value = a.startTime  ?: ""
        _endTime.value   = a.endTime    ?: ""
        _duration.value  = a.duration   ?: ""
        _status.value    = a.status
        _priority.value  = a.priority
        _notes.value     = a.notes      ?: ""
        _isExercise.value = a.isExercise
        _distance.value  = a.distance   ?: ""
        _calories.value  = a.calories?.toString() ?: ""
    }

    // ─── Field Updaters ───────────────────────────────────────────────────────

    fun onActivityNameChange(v: String)   { _activityName.value = v; _nameError.value = null }
    fun onCategoryChange(v: String)       { _category.value = v }
    fun onCustomCategoryChange(v: String) { _customCategory.value = v }
    fun onDateChange(v: LocalDate)        { _date.value = v }
    fun onStartTimeChange(v: String)      { _startTime.value = v; autoComputeDuration() }
    fun onEndTimeChange(v: String)        { _endTime.value = v; autoComputeDuration() }
    fun onDurationChange(v: String)       { _duration.value = v }
    fun onStatusChange(v: ActivityStatus) { _status.value = v }
    fun onPriorityChange(v: ActivityPriority) { _priority.value = v }
    fun onNotesChange(v: String)          { _notes.value = v }
    fun onIsExerciseChange(v: Boolean)    { _isExercise.value = v }
    fun onExerciseTypeChange(v: String)   { _exerciseType.value = v }
    fun onDistanceChange(v: String)       { _distance.value = v }
    fun onCaloriesChange(v: String)       { _calories.value = v }

    /**
     * Auto-compute duration when both start and end time are filled.
     * Handles HH:mm format.
     */
    private fun autoComputeDuration() {
        val start = _startTime.value.trim()
        val end   = _endTime.value.trim()
        if (start.isBlank() || end.isBlank()) return

        try {
            val (sh, sm) = start.split(":").map { it.toInt() }
            val (eh, em) = end.split(":").map { it.toInt() }
            val startMinutes = sh * 60 + sm
            var endMinutes   = eh * 60 + em
            if (endMinutes < startMinutes) endMinutes += 24 * 60 // next day
            val diff = endMinutes - startMinutes
            _duration.value = repo.formatMinutes(diff)
        } catch (_: Exception) {
            // Invalid time format — leave duration as-is
        }
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    fun save() {
        if (!validate()) return

        val resolvedCategory = if (_category.value == "Custom" && _customCategory.value.isNotBlank())
            _customCategory.value.trim()
        else _category.value

        viewModelScope.launch {
            _uiState.value = AddEditUiState.Saving

            if (isEditMode && originalActivity != null) {
                // Edit mode: update existing
                val updated = originalActivity!!.copy(
                    date         = _date.value,
                    day          = DailyActivity.dayNameFromDate(_date.value),
                    activityName = _activityName.value.trim(),
                    category     = resolvedCategory,
                    startTime    = _startTime.value.trim().ifBlank { null },
                    endTime      = _endTime.value.trim().ifBlank { null },
                    duration     = _duration.value.trim().ifBlank { null },
                    status       = _status.value,
                    priority     = _priority.value,
                    notes        = _notes.value.trim().ifBlank { null },
                    isExercise   = _isExercise.value,
                    distance     = _distance.value.trim().ifBlank { null },
                    calories     = _calories.value.trim().toIntOrNull()
                ).asUpdated()
                repo.updateActivity(updated)
            } else {
                // Add mode: create new
                repo.addActivity(
                    activityName = _activityName.value.trim(),
                    category     = resolvedCategory,
                    date         = _date.value,
                    startTime    = _startTime.value.trim().ifBlank { null },
                    endTime      = _endTime.value.trim().ifBlank { null },
                    duration     = _duration.value.trim().ifBlank { null },
                    status       = _status.value,
                    priority     = _priority.value,
                    notes        = _notes.value.trim().ifBlank { null },
                    isExercise   = _isExercise.value,
                    distance     = _distance.value.trim().ifBlank { null },
                    calories     = _calories.value.trim().toIntOrNull()
                )
            }

            _uiState.value = AddEditUiState.Saved
        }
    }

    private fun validate(): Boolean {
        var valid = true
        if (_activityName.value.trim().isBlank()) {
            _nameError.value = "Activity name is required"
            valid = false
        }
        return valid
    }

    fun resetSavedState() {
        if (_uiState.value == AddEditUiState.Saved) {
            _uiState.value = AddEditUiState.Idle
        }
    }
}

sealed class AddEditUiState {
    object Idle   : AddEditUiState()
    object Loading: AddEditUiState()
    object Saving : AddEditUiState()
    object Saved  : AddEditUiState()
    data class Error(val message: String) : AddEditUiState()
}
