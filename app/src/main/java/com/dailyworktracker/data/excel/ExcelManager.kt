package com.dailyworktracker.data.excel

import android.content.Context
import android.net.Uri
import com.dailyworktracker.data.model.ActivityPriority
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.DailyActivity
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Handles all Apache POI operations against Daily Work.xlsx via Android SAF.
 *
 * Column layout (0-indexed):
 *   0=ID  1=Date  2=Day  3=Activity  4=Category  5=StartTime  6=EndTime
 *   7=Duration  8=Status  9=Priority  10=Notes  11=Exercise
 *   12=Distance  13=Calories  14=CreatedAt  15=UpdatedAt
 *
 * Write strategy: read → modify in memory → write via temp file → copy to URI.
 * This is the most reliable approach for SAF on all Android versions.
 */
class ExcelManager(private val context: Context) {

    companion object {
        const val SHEET_NAME         = "Daily Work"
        private const val HEADER_ROW = 0
        private const val DATA_START  = 1

        // Column indices
        private const val COL_ID         = 0
        private const val COL_DATE       = 1
        private const val COL_DAY        = 2
        private const val COL_ACTIVITY   = 3
        private const val COL_CATEGORY   = 4
        private const val COL_START_TIME = 5
        private const val COL_END_TIME   = 6
        private const val COL_DURATION   = 7
        private const val COL_STATUS     = 8
        private const val COL_PRIORITY   = 9
        private const val COL_NOTES      = 10
        private const val COL_EXERCISE   = 11
        private const val COL_DISTANCE   = 12
        private const val COL_CALORIES   = 13
        private const val COL_CREATED_AT = 14
        private const val COL_UPDATED_AT = 15
        private const val TOTAL_COLS     = 16

        private val DATE_FMT     = DateTimeFormatter.ISO_LOCAL_DATE
        private val DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    // ─── Public Read API ──────────────────────────────────────────────────────

    /**
     * Read all activities from the given SAF URI.
     * Validates that:
     *   1. The URI is openable
     *   2. The file is a valid .xlsx workbook
     *   3. The "Daily Work" sheet exists
     */
    fun readAllActivities(uri: Uri): ExcelResult<List<DailyActivity>> {
        return try {
            val workbook = openWorkbook(uri) ?: return ExcelResult.invalidFile()
            val sheet = workbook.getSheet(SHEET_NAME) ?: return ExcelResult.sheetNotFound()
            val activities = mutableListOf<DailyActivity>()

            for (rowIndex in DATA_START..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                // Skip rows where ID cell is blank (empty trailing rows)
                val id = getCellString(row, COL_ID).trim()
                if (id.isBlank()) continue

                parseRowToActivity(row)?.let { activities.add(it) }
            }

            workbook.close()
            ExcelResult.success(activities)
        } catch (e: org.apache.poi.openxml4j.exceptions.InvalidFormatException) {
            ExcelResult.invalidFile()
        } catch (e: org.apache.poi.EmptyFileException) {
            ExcelResult.invalidFile()
        } catch (e: IOException) {
            ExcelResult.Error.Generic("Cannot read file: ${e.message}", e)
        } catch (e: Exception) {
            ExcelResult.Error.Generic("Unexpected error reading Excel: ${e.message}", e)
        }
    }

    /**
     * Get the maximum existing activity number (numeric part of ACT-XXXXXX).
     * Returns 0 if no activities exist.
     */
    fun getMaxActivityNumber(uri: Uri): Int {
        return try {
            val workbook = openWorkbook(uri) ?: return 0
            val sheet = workbook.getSheet(SHEET_NAME) ?: return 0
            var maxNum = 0
            for (rowIndex in DATA_START..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val id = getCellString(row, COL_ID).trim()
                val num = DailyActivity.parseIdNumber(id)
                if (num > maxNum) maxNum = num
            }
            workbook.close()
            maxNum
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Validate that a URI points to a valid workbook with the "Daily Work" sheet.
     */
    fun validate(uri: Uri): ExcelResult<Unit> {
        return try {
            val workbook = openWorkbook(uri) ?: return ExcelResult.invalidFile()
            val hasSheet = workbook.getSheet(SHEET_NAME) != null
            workbook.close()
            if (hasSheet) ExcelResult.success(Unit) else ExcelResult.sheetNotFound()
        } catch (e: Exception) {
            ExcelResult.invalidFile()
        }
    }

    // ─── Public Write API ─────────────────────────────────────────────────────

    /**
     * Append a new activity as a new row at the bottom of the sheet.
     * NEVER overwrites an existing row.
     */
    fun appendActivity(uri: Uri, activity: DailyActivity): ExcelResult<Unit> {
        return modifyWorkbook(uri) { sheet ->
            val newRowNum = findLastDataRow(sheet) + 1
            val row = sheet.createRow(newRowNum)
            writeActivityToRow(sheet.workbook, row, activity)
        }
    }

    /**
     * Update the row matching [activity.id] in place.
     * Returns [ExcelResult.Error.RowNotFound] if no row has that ID.
     */
    fun updateActivity(uri: Uri, activity: DailyActivity): ExcelResult<Unit> {
        var found = false
        val result = modifyWorkbook(uri) { sheet ->
            for (rowIndex in DATA_START..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                if (getCellString(row, COL_ID).trim() == activity.id) {
                    writeActivityToRow(sheet.workbook, row, activity)
                    found = true
                    break
                }
            }
        }
        return if (result is ExcelResult.Success && !found)
            ExcelResult.Error.RowNotFound(activity.id)
        else result
    }

    /**
     * Delete the row matching [activityId] and shift subsequent rows up.
     * Returns [ExcelResult.Error.RowNotFound] if no row has that ID.
     */
    fun deleteActivity(uri: Uri, activityId: String): ExcelResult<Unit> {
        var found = false
        val result = modifyWorkbook(uri) { sheet ->
            for (rowIndex in DATA_START..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                if (getCellString(row, COL_ID).trim() == activityId) {
                    sheet.removeRow(row)
                    // Shift remaining rows up to fill the gap
                    if (rowIndex < sheet.lastRowNum) {
                        sheet.shiftRows(rowIndex + 1, sheet.lastRowNum, -1)
                    }
                    found = true
                    break
                }
            }
        }
        return if (result is ExcelResult.Success && !found)
            ExcelResult.Error.RowNotFound(activityId)
        else result
    }

    /**
     * Create a timestamped backup copy of the workbook.
     * Returns the path of the backup file in internal cache.
     */
    fun createBackup(uri: Uri): ExcelResult<File> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ExcelResult.saveFailed()
            val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val backupFile = File(context.cacheDir, "Daily Work_Backup_$timestamp.xlsx")
            backupFile.outputStream().use { out ->
                inputStream.use { it.copyTo(out) }
            }
            ExcelResult.success(backupFile)
        } catch (e: Exception) {
            ExcelResult.saveFailed(e)
        }
    }

    /**
     * Create a new empty workbook with the correct header row and save it to [uri].
     */
    fun createNewSheet(uri: Uri): ExcelResult<Unit> {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(SHEET_NAME)
            val headerStyle = createHeaderStyle(workbook)
            writeHeaderRow(sheet, headerStyle)
            saveWorkbookToUri(workbook, uri)
            ExcelResult.success(Unit)
        } catch (e: Exception) {
            ExcelResult.saveFailed(e)
        }
    }

    // ─── Internal: Read Helpers ───────────────────────────────────────────────

    private fun openWorkbook(uri: Uri): Workbook? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            inputStream.use { WorkbookFactory.create(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRowToActivity(row: Row): DailyActivity? {
        return try {
            val id   = getCellString(row, COL_ID).trim()
            val dateStr = getCellString(row, COL_DATE).trim()
            val date = parseDate(dateStr) ?: return null // Skip rows with no valid date

            DailyActivity(
                id           = id,
                date         = date,
                day          = getCellString(row, COL_DAY).ifBlank {
                                   DailyActivity.dayNameFromDate(date)
                               },
                activityName = getCellString(row, COL_ACTIVITY).trim(),
                category     = getCellString(row, COL_CATEGORY).trim().ifBlank { "Other" },
                startTime    = getCellString(row, COL_START_TIME).trim().ifBlank { null },
                endTime      = getCellString(row, COL_END_TIME).trim().ifBlank { null },
                duration     = getCellString(row, COL_DURATION).trim().ifBlank { null },
                status       = ActivityStatus.fromString(
                                   getCellString(row, COL_STATUS).trim()
                               ),
                priority     = ActivityPriority.fromString(
                                   getCellString(row, COL_PRIORITY).trim()
                               ),
                notes        = getCellString(row, COL_NOTES).trim().ifBlank { null },
                isExercise   = parseBool(getCellString(row, COL_EXERCISE)),
                distance     = getCellString(row, COL_DISTANCE).trim().ifBlank { null },
                calories     = getCellString(row, COL_CALORIES).trim()
                                   .toDoubleOrNull()?.toInt(),
                createdAt    = parseDateTime(getCellString(row, COL_CREATED_AT))
                                   ?: LocalDateTime.now(),
                updatedAt    = parseDateTime(getCellString(row, COL_UPDATED_AT))
                                   ?: LocalDateTime.now()
            )
        } catch (e: Exception) {
            null // Skip malformed rows
        }
    }

    /** Safe cell reader — handles all POI cell types and returns a String. */
    private fun getCellString(row: Row, colIndex: Int): String {
        val cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
            ?: return ""
        return when (cell.cellType) {
            CellType.STRING  -> cell.stringCellValue ?: ""
            CellType.NUMERIC -> {
                // POI stores dates as doubles; check if it's an Excel date
                if (DateUtil.isCellDateFormatted(cell)) {
                    val localDate = cell.localDateTimeCellValue?.toLocalDate()
                    localDate?.format(DATE_FMT) ?: cell.numericCellValue.toLong().toString()
                } else {
                    // Return as integer string if it's a whole number, else decimal
                    val d = cell.numericCellValue
                    if (d == kotlin.math.floor(d) && !d.isInfinite())
                        d.toLong().toString()
                    else d.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                // Evaluate formula result
                try { cell.stringCellValue } catch (_: Exception) {
                    try { cell.numericCellValue.toLong().toString() } catch (_: Exception) { "" }
                }
            }
            CellType.BLANK   -> ""
            else             -> ""
        }
    }

    private fun parseDate(value: String): LocalDate? {
        if (value.isBlank()) return null
        // Try ISO format first
        return try {
            LocalDate.parse(value, DATE_FMT)
        } catch (e: DateTimeParseException) {
            // Try dd/MM/yyyy or MM/dd/yyyy
            val patterns = listOf("dd/MM/yyyy", "MM/dd/yyyy", "d/M/yyyy", "yyyy/MM/dd")
            for (pattern in patterns) {
                try {
                    return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern))
                } catch (_: Exception) {}
            }
            null
        }
    }

    private fun parseDateTime(value: String): LocalDateTime? {
        if (value.isBlank()) return null
        return try {
            LocalDateTime.parse(value, DATETIME_FMT)
        } catch (e: DateTimeParseException) {
            // Fallback: try just a date
            try {
                LocalDate.parse(value, DATE_FMT).atStartOfDay()
            } catch (_: Exception) { null }
        }
    }

    private fun parseBool(value: String): Boolean =
        value.trim().lowercase() in listOf("true", "yes", "1", "y")

    // ─── Internal: Write Helpers ──────────────────────────────────────────────

    /**
     * Opens the workbook, applies [block] to the target sheet, and saves back.
     * Uses a temp file for reliability on all Android versions.
     */
    private fun modifyWorkbook(uri: Uri, block: (Sheet) -> Unit): ExcelResult<Unit> {
        return try {
            // 1. Read
            val workbook = openWorkbook(uri) ?: return ExcelResult.invalidFile()
            val sheet = workbook.getSheet(SHEET_NAME) ?: run {
                workbook.close()
                return ExcelResult.sheetNotFound()
            }

            // 2. Modify
            block(sheet)

            // 3. Write back
            saveWorkbookToUri(workbook, uri)
            ExcelResult.success(Unit)
        } catch (e: IOException) {
            ExcelResult.saveFailed(e)
        } catch (e: Exception) {
            ExcelResult.Error.Generic("Excel write error: ${e.message}", e)
        }
    }

    /**
     * Writes the workbook to internal cache first, then copies to the SAF URI.
     * This avoids issues with streaming directly to a content URI.
     */
    private fun saveWorkbookToUri(workbook: Workbook, uri: Uri) {
        val tempFile = File(context.cacheDir, "dwt_temp_write.xlsx")
        try {
            tempFile.outputStream().buffered().use { out ->
                workbook.write(out)
                out.flush()
            }
            workbook.close()

            // Copy temp to URI (truncate mode)
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                tempFile.inputStream().use { it.copyTo(out) }
                out.flush()
            } ?: throw IOException("Cannot open output stream for URI")
        } finally {
            tempFile.delete()
        }
    }

    private fun writeActivityToRow(workbook: Workbook, row: Row, activity: DailyActivity) {
        // Ensure the row has enough cells
        for (i in 0 until TOTAL_COLS) {
            if (row.getCell(i) == null) row.createCell(i)
        }

        setString(row, COL_ID,         activity.id)
        setString(row, COL_DATE,       activity.date.format(DATE_FMT))
        setString(row, COL_DAY,        activity.day)
        setString(row, COL_ACTIVITY,   activity.activityName)
        setString(row, COL_CATEGORY,   activity.category)
        setString(row, COL_START_TIME, activity.startTime ?: "")
        setString(row, COL_END_TIME,   activity.endTime   ?: "")
        setString(row, COL_DURATION,   activity.duration  ?: "")
        setString(row, COL_STATUS,     activity.status.displayName)
        setString(row, COL_PRIORITY,   activity.priority.displayName)
        setString(row, COL_NOTES,      activity.notes     ?: "")
        setString(row, COL_EXERCISE,   if (activity.isExercise) "Yes" else "No")
        setString(row, COL_DISTANCE,   activity.distance  ?: "")

        if (activity.calories != null) {
            row.getCell(COL_CALORIES).setCellValue(activity.calories.toDouble())
        } else {
            setString(row, COL_CALORIES, "")
        }

        setString(row, COL_CREATED_AT, activity.createdAt.format(DATETIME_FMT))
        setString(row, COL_UPDATED_AT, activity.updatedAt.format(DATETIME_FMT))
    }

    private fun setString(row: Row, colIndex: Int, value: String) {
        (row.getCell(colIndex) ?: row.createCell(colIndex)).setCellValue(value)
    }

    private fun writeHeaderRow(sheet: Sheet, headerStyle: CellStyle) {
        val headers = listOf(
            "ID", "Date", "Day", "Activity", "Category",
            "Start Time", "End Time", "Duration", "Status", "Priority",
            "Notes", "Exercise", "Distance", "Calories", "Created At", "Updated At"
        )
        val row = sheet.createRow(HEADER_ROW)
        headers.forEachIndexed { i, header ->
            val cell = row.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
            sheet.setColumnWidth(i, when (i) {
                COL_ID         -> 4000
                COL_DATE       -> 3200
                COL_DAY        -> 3200
                COL_ACTIVITY   -> 8000
                COL_CATEGORY   -> 4000
                COL_NOTES      -> 8000
                COL_CREATED_AT,
                COL_UPDATED_AT -> 6000
                else           -> 3200
            })
        }
    }

    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        style.setFont(font)
        return style
    }

    /** Find the last row that has actual data (not counting trailing blank rows). */
    private fun findLastDataRow(sheet: Sheet): Int {
        var last = 0
        for (rowIndex in DATA_START..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if (getCellString(row, COL_ID).isNotBlank()) last = rowIndex
        }
        return last
    }
}
