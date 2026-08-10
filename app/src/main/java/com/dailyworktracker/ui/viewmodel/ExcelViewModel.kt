package com.dailyworktracker.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.data.excel.ExcelResult
import com.dailyworktracker.data.preferences.UserPreferencesRepository
import com.dailyworktracker.data.repository.ActivityRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel that manages the Excel file connection lifecycle.
 * Shared across screens that need to know about file state (Home, ExcelManagement).
 */
class ExcelViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: ActivityRepository = application.appContainer.activityRepository
    private val prefs: UserPreferencesRepository = application.appContainer.userPreferencesRepository

    // ─── UI State ─────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<ExcelUiState>(ExcelUiState.Idle)
    val uiState: StateFlow<ExcelUiState> = _uiState.asStateFlow()

    val isFileConnected: StateFlow<Boolean> = prefs.excelFileUri
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastUpdated: StateFlow<Long> = prefs.excelLastUpdated
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _connectedFileName = MutableStateFlow<String?>(null)
    val connectedFileName: StateFlow<String?> = _connectedFileName.asStateFlow()

    // ─── Startup ──────────────────────────────────────────────────────────────

    init {
        // On ViewModel creation, attempt to reload from the persisted file
        viewModelScope.launch {
            loadConnectedFileName()
            autoReloadIfConnected()
        }
    }

    private suspend fun autoReloadIfConnected() {
        _uiState.value = ExcelUiState.Loading("Loading data…")
        val result = repo.reloadFromConnectedExcel()
        _uiState.value = when (result) {
            is ExcelResult.Success  -> ExcelUiState.Loaded(result.data)
            is ExcelResult.Error.NoFileUploaded -> ExcelUiState.NoFile
            is ExcelResult.Error    -> ExcelUiState.Error(result.errorMessage())
        }
    }

    private suspend fun loadConnectedFileName() {
        _connectedFileName.value = repo.getConnectedFileName()
    }

    // ─── File Connection ──────────────────────────────────────────────────────

    /**
     * Called after the SAF file picker returns a URI.
     * Takes persistable permission, connects the file, and loads data.
     */
    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ExcelUiState.Loading("Reading Excel file…")

            // Take persistable read+write permission so URI survives reboots
            try {
                getApplication<android.app.Application>()
                    .contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
            } catch (e: Exception) {
                // Permission may already be held — not fatal
            }

            val result = repo.connectExcelFile(uri)
            loadConnectedFileName()
            _uiState.value = when (result) {
                is ExcelResult.Success -> ExcelUiState.Loaded(result.data)
                is ExcelResult.Error   -> ExcelUiState.Error(result.errorMessage())
            }
        }
    }

    /** Reload from the already-connected file (e.g. pull-to-refresh). */
    fun reload() {
        viewModelScope.launch {
            _uiState.value = ExcelUiState.Loading("Refreshing…")
            val result = repo.reloadFromConnectedExcel()
            _uiState.value = when (result) {
                is ExcelResult.Success -> ExcelUiState.Loaded(result.data)
                is ExcelResult.Error   -> ExcelUiState.Error(result.errorMessage())
            }
        }
    }

    /** Full save: write all Room data back to Excel. */
    fun saveAll() {
        viewModelScope.launch {
            _uiState.value = ExcelUiState.Loading("Saving to Excel…")
            val result = repo.saveAllToExcel()
            _uiState.value = when (result) {
                is ExcelResult.Success -> ExcelUiState.Saved(result.data)
                is ExcelResult.Error   -> ExcelUiState.Error(result.errorMessage())
            }
        }
    }

    /** Create a timestamped backup file, returns it for SAF share/save. */
    fun createBackup(onResult: (ExcelResult<java.io.File>) -> Unit) {
        viewModelScope.launch {
            val result = repo.createBackup()
            onResult(result)
        }
    }

    /** Disconnect the current file. Room data is preserved. */
    fun disconnect() {
        viewModelScope.launch {
            repo.disconnectExcelFile()
            _connectedFileName.value = null
            _uiState.value = ExcelUiState.NoFile
        }
    }

    fun clearError() {
        if (_uiState.value is ExcelUiState.Error) {
            _uiState.value = ExcelUiState.Idle
        }
    }
}

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class ExcelUiState {
    object Idle                        : ExcelUiState()
    object NoFile                      : ExcelUiState()
    data class Loading(val message: String) : ExcelUiState()
    data class Loaded(val count: Int)  : ExcelUiState()
    data class Saved(val count: Int)   : ExcelUiState()
    data class Error(val message: String)   : ExcelUiState()
}
