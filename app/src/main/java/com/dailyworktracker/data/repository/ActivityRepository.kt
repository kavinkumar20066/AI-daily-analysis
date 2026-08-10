package com.dailyworktracker.data.repository

import android.content.Context
import android.net.Uri
import com.dailyworktracker.data.db.ActivityDao
import com.dailyworktracker.data.db.CategoryCount
import com.dailyworktracker.data.db.DailyCount
import com.dailyworktracker.data.excel.ExcelManager
import com.dailyworktracker.data.excel.ExcelResult
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.DailyActivity
import com.dailyworktracker.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ActivityRepository — single source of truth coordinator.
 *
 * Room is the local cache used by all UI reads (via Flow).
 * Daily Work.xlsx is the permanent source of truth.
 *
 * Write contract:
 *   Add:    insert into Room THEN append row to Excel.
 *   Edit:   update in Room THEN update matching row in Excel (by ID, never by position).
 *   Delete: delete from Room THEN delete matching row in Excel (by ID).
 *
 * If Excel is not connected, Room operations still succeed (offline mode).
 * When Excel reconnects, user can trigger a full re-sync.
 */
class ActivityRepository(
    private val context: Context,
    private val activityDao: ActivityDao,
    private val excelManager: ExcelManager,
    private val prefsRepo: UserPreferencesRepository
) {

    // ─── Flow Accessors (from Room) ───────────────────────────────────────────

    fun getActivitiesForDate(date: LocalDate): Flow<List<DailyActivity>> =
        activityDao.getActivitiesForDate(date.toString())

    fun getActivitiesForDateRange(start: LocalDate, end: LocalDate): Flow<List<DailyActivity>> =
        activityDao.getActivitiesForDateRange(start.toString(), end.toString())

    fun getActivitiesForMonth(yearMonth: String): Flow<List<DailyActivity>> =
        activityDao.getActivitiesForMonth(yearMonth)

    fun getActivitiesForYear(year: String): Flow<List<DailyActivity>> =
        activityDao.getActivitiesForYear(year)

    fun getAllActivities(): Flow<List<DailyActivity>> =
        activityDao.getAllActivities()

    fun getAllActivityDates(): Flow<List<String>> =
        activityDao.getAllActivityDates()

    fun getAllCategories(): Flow<List<String>> =
        activityDao.getAllCategories()

    fun searchActivities(query: String): Flow<List<DailyActivity>> =
        activityDao.searchActivities(query)

    fun getFilteredActivities(
        query: String = "",
        status: String = "",
        category: String = "",
        isExerciseOnly: Boolean = false
    ): Flow<List<DailyActivity>> =
        activityDao.getFilteredActivities(
            query = query,
            status = status,
            category = category,
            isExerciseOnly = if (isExerciseOnly) 1 else 0
        )

    fun getTotalCountForDate(date: LocalDate): Flow<Int> =
        activityDao.getTotalCountForDate(date.toString())

    fun getCompletedCountForDate(date: LocalDate): Flow<Int> =
        activityDao.getCompletedCountForDate(date.toString())

    fun getRecentExerciseActivities(limit: Int = 10): Flow<List<DailyActivity>> =
        activityDao.getRecentExerciseActivities(limit)

    // ─── Suspend Accessors (analytics) ───────────────────────────────────────

    suspend fun getCategoryCountsForMonth(yearMonth: String): List<CategoryCount> =
        withContext(Dispatchers.IO) { activityDao.getCategoryCountsForMonth(yearMonth) }

    suspend fun getDailyCountsForMonth(yearMonth: String): List<DailyCount> =
        withContext(Dispatchers.IO) { activityDao.getDailyCountsForMonth(yearMonth) }

    suspend fun getActivitiesForDateSync(date: LocalDate): List<DailyActivity> =
        withContext(Dispatchers.IO) { activityDao.getActivitiesForDateSync(date.toString()) }

    suspend fun getActivitiesForYearSync(year: String): List<DailyActivity> =
        withContext(Dispatchers.IO) { activityDao.getActivitiesForYearSync(year) }

    suspend fun getActivityById(id: String): DailyActivity? =
        withContext(Dispatchers.IO) { activityDao.getActivityById(id) }

    // ─── Excel Load ───────────────────────────────────────────────────────────

    /**
     * Load the Excel file into Room.
     * Replaces all existing Room data with the Excel content.
     * Returns the number of activities loaded.
     */
    suspend fun loadFromExcel(uri: Uri): ExcelResult<Int> = withContext(Dispatchers.IO) {
        val readResult = excelManager.readAllActivities(uri)
        if (readResult is ExcelResult.Error) return@withContext readResult

        val activities = (readResult as ExcelResult.Success).data
        activityDao.deleteAllActivities()
        activityDao.insertActivities(activities)
        ExcelResult.success(activities.size)
    }

    /**
     * Reload from the currently connected Excel file.
     * No-op (returns NoFileUploaded) if no file is connected.
     */
    suspend fun reloadFromConnectedExcel(): ExcelResult<Int> = withContext(Dispatchers.IO) {
        val uriString = prefsRepo.excelFileUri.firstOrNull()
        if (uriString.isNullOrBlank()) return@withContext ExcelResult.noFile()
        loadFromExcel(Uri.parse(uriString))
    }

    // ─── CRUD with Excel Sync ─────────────────────────────────────────────────

    /**
     * Add a new activity.
     * 1. Generate ID
     * 2. Insert into Room
     * 3. Append row to Excel (if connected)
     */
    suspend fun addActivity(
        activityName: String,
        category: String,
        date: LocalDate,
        startTime: String?,
        endTime: String?,
        duration: String?,
        status: ActivityStatus,
        priority: com.dailyworktracker.data.model.ActivityPriority,
        notes: String?,
        isExercise: Boolean,
        distance: String?,
        calories: Int?
    ): ExcelResult<DailyActivity> = withContext(Dispatchers.IO) {
        // Generate next ID
        val nextId = generateNextId()

        val activity = DailyActivity.create(
            id           = nextId,
            date         = date,
            activityName = activityName,
            category     = category,
            startTime    = startTime,
            endTime      = endTime,
            duration     = duration,
            status       = status,
            priority     = priority,
            notes        = notes,
            isExercise   = isExercise,
            distance     = distance,
            calories     = calories
        )

        // 1. Room insert (always succeeds offline)
        activityDao.insertActivity(activity)

        // 2. Excel append (only if connected)
        val uriString = prefsRepo.excelFileUri.firstOrNull()
        if (!uriString.isNullOrBlank()) {
            val uri = Uri.parse(uriString)
            val writeResult = excelManager.appendActivity(uri, activity)
            if (writeResult is ExcelResult.Error) {
                // Room write succeeded — log the error but don't roll back Room
                // The data is safe in Room and will sync on next reconcile
            }
        }

        ExcelResult.success(activity)
    }

    /**
     * Update an existing activity.
     * 1. Update in Room
     * 2. Update matching Excel row by ID
     */
    suspend fun updateActivity(activity: DailyActivity): ExcelResult<Unit> =
        withContext(Dispatchers.IO) {
            val updated = activity.asUpdated()

            // 1. Room update
            activityDao.updateActivity(updated)

            // 2. Excel update
            val uriString = prefsRepo.excelFileUri.firstOrNull()
            if (!uriString.isNullOrBlank()) {
                excelManager.updateActivity(Uri.parse(uriString), updated)
            }

            ExcelResult.success(Unit)
        }

    /**
     * Update just the status of an activity (quick toggle).
     */
    suspend fun updateActivityStatus(
        activity: DailyActivity,
        newStatus: ActivityStatus
    ): ExcelResult<Unit> =
        updateActivity(activity.withStatus(newStatus))

    /**
     * Delete an activity permanently.
     * 1. Delete from Room
     * 2. Delete matching Excel row by ID
     */
    suspend fun deleteActivity(activity: DailyActivity): ExcelResult<Unit> =
        withContext(Dispatchers.IO) {
            // 1. Room delete
            activityDao.deleteActivity(activity)

            // 2. Excel delete
            val uriString = prefsRepo.excelFileUri.firstOrNull()
            if (!uriString.isNullOrBlank()) {
                excelManager.deleteActivity(Uri.parse(uriString), activity.id)
            }

            ExcelResult.success(Unit)
        }

    // ─── Excel File Management ────────────────────────────────────────────────

    /**
     * Connect a new Excel file via SAF URI.
     * Validates the file, saves the URI to DataStore, and loads data into Room.
     *
     * Call [Context.takePersistableUriPermission] before this to maintain access
     * across reboots.
     */
    suspend fun connectExcelFile(uri: Uri): ExcelResult<Int> = withContext(Dispatchers.IO) {
        // Validate first
        val validation = excelManager.validate(uri)
        if (validation is ExcelResult.Error) {
            // Check if it's a "sheet not found" — offer to create the sheet
            if (validation is ExcelResult.Error.SheetNotFound) {
                // Create the sheet in the workbook
                val createResult = excelManager.createNewSheet(uri)
                if (createResult is ExcelResult.Error) return@withContext createResult
            } else {
                return@withContext validation
            }
        }

        // Persist URI
        prefsRepo.saveExcelFileUri(uri.toString())

        // Load into Room
        loadFromExcel(uri)
    }

    /**
     * Disconnect the current Excel file.
     * Room data is preserved; only the URI reference is removed.
     */
    suspend fun disconnectExcelFile() {
        prefsRepo.clearExcelFileUri()
    }

    /**
     * Save all current Room data back to the connected Excel file.
     * Full overwrite — re-creates all rows.
     */
    suspend fun saveAllToExcel(): ExcelResult<Int> = withContext(Dispatchers.IO) {
        val uriString = prefsRepo.excelFileUri.firstOrNull()
        if (uriString.isNullOrBlank()) return@withContext ExcelResult.noFile()

        val uri = Uri.parse(uriString)
        val activities = activityDao.getAllActivities().firstOrNull() ?: emptyList()

        try {
            // Re-write the entire sheet
            val createResult = excelManager.createNewSheet(uri)
            if (createResult is ExcelResult.Error) return@withContext createResult

            for (activity in activities) {
                val appendResult = excelManager.appendActivity(uri, activity)
                if (appendResult is ExcelResult.Error) return@withContext appendResult
            }

            prefsRepo.saveExcelFileUri(uriString) // refresh last-updated timestamp
            ExcelResult.success(activities.size)
        } catch (e: Exception) {
            ExcelResult.saveFailed(e)
        }
    }

    /**
     * Create a timestamped backup copy of the Excel file.
     * Returns the backup file for sharing/saving via SAF.
     */
    suspend fun createBackup(): ExcelResult<java.io.File> = withContext(Dispatchers.IO) {
        val uriString = prefsRepo.excelFileUri.firstOrNull()
        if (uriString.isNullOrBlank()) return@withContext ExcelResult.noFile()
        excelManager.createBackup(Uri.parse(uriString))
    }

    /**
     * Get the connected file name (display only) from the URI.
     */
    suspend fun getConnectedFileName(): String? {
        val uriString = prefsRepo.excelFileUri.firstOrNull() ?: return null
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            "Daily Work.xlsx"
        }
    }

    // ─── ID Generation ────────────────────────────────────────────────────────

    /**
     * Generate the next sequential activity ID.
     * Checks both Room and the Excel file to ensure no collisions.
     */
    suspend fun generateNextId(): String = withContext(Dispatchers.IO) {
        val roomMax = activityDao.getMaxActivityNumber() ?: 0
        val excelMax = try {
            val uriString = prefsRepo.excelFileUri.firstOrNull()
            if (!uriString.isNullOrBlank())
                excelManager.getMaxActivityNumber(Uri.parse(uriString))
            else 0
        } catch (e: Exception) { 0 }

        val nextNum = maxOf(roomMax, excelMax) + 1
        DailyActivity.generateId(nextNum)
    }

    // ─── Analytics Helpers ────────────────────────────────────────────────────

    /**
     * Compute completion rate for a date: completedCount / totalCount * 100.
     * Returns 0.0 if no activities.
     */
    suspend fun completionRateForDate(date: LocalDate): Float = withContext(Dispatchers.IO) {
        val activities = activityDao.getActivitiesForDateSync(date.toString())
        if (activities.isEmpty()) return@withContext 0f
        val completed = activities.count { it.status == ActivityStatus.Completed }
        (completed.toFloat() / activities.size) * 100f
    }

    /**
     * Compute completion rate for a month (YYYY-MM format).
     */
    suspend fun completionRateForMonth(yearMonth: String): Float = withContext(Dispatchers.IO) {
        val total     = activityDao.getTotalCountForMonth(yearMonth)
        val completed = activityDao.getCompletedCountForMonth(yearMonth)
        if (total == 0) 0f else (completed.toFloat() / total) * 100f
    }

    /**
     * Compute productive hours for a date from duration strings.
     * Parses formats like "1h 30m", "90m", "2h", "1.5h".
     */
    suspend fun productiveHoursForDate(date: LocalDate): Float = withContext(Dispatchers.IO) {
        val activities = activityDao.getActivitiesForDateSync(date.toString())
        activities
            .filter { it.status == ActivityStatus.Completed }
            .sumOf { parseDurationToMinutes(it.duration ?: "").toDouble() }
            .toFloat() / 60f
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    /**
     * Parse a duration string into total minutes.
     * Handles: "1h 30m", "90m", "2h", "1.5h", "90", "1:30"
     */
    fun parseDurationToMinutes(duration: String): Int {
        if (duration.isBlank()) return 0
        val d = duration.trim().lowercase()

        // "1h 30m" or "1h30m"
        val hm = Regex("""(\d+(?:\.\d+)?)\s*h\s*(\d+)\s*m""").find(d)
        if (hm != null) {
            return (hm.groupValues[1].toFloat() * 60).toInt() + hm.groupValues[2].toInt()
        }

        // "2h" or "1.5h"
        val h = Regex("""(\d+(?:\.\d+)?)\s*h""").find(d)
        if (h != null) return (h.groupValues[1].toFloat() * 60).toInt()

        // "90m"
        val m = Regex("""(\d+)\s*m""").find(d)
        if (m != null) return m.groupValues[1].toInt()

        // "1:30" (hours:minutes)
        val colon = Regex("""(\d+):(\d{2})""").find(d)
        if (colon != null) {
            return colon.groupValues[1].toInt() * 60 + colon.groupValues[2].toInt()
        }

        // Plain number — assume minutes
        return d.toIntOrNull() ?: 0
    }

    /**
     * Format minutes as a readable duration string, e.g. "2h 30m".
     */
    fun formatMinutes(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0m"
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else   -> "${h}h ${m}m"
        }
    }
}
