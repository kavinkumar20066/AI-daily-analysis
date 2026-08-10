package com.dailyworktracker.data.excel

import android.net.Uri

/**
 * Sealed result type for all Excel operations.
 * Every repository method that touches the Excel file returns one of these.
 */
sealed class ExcelResult<out T> {

    data class Success<T>(val data: T) : ExcelResult<T>()

    sealed class Error : ExcelResult<Nothing>() {
        /** User has not uploaded an Excel file yet. */
        object NoFileUploaded : Error()

        /** The selected file is not a valid Excel workbook. */
        object InvalidFile : Error()

        /** The workbook exists but the "Daily Work" sheet is missing. */
        object SheetNotFound : Error()

        /** A write operation failed (disk full, permissions, etc.). */
        data class SaveFailure(val cause: Throwable? = null) : Error()

        /** Row with the given ID was not found when trying to edit/delete. */
        data class RowNotFound(val id: String) : Error()

        /** Generic catch-all with message. */
        data class Generic(val message: String, val cause: Throwable? = null) : Error()
    }

    val isSuccess get() = this is Success
    val isError   get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun errorMessage(): String = when (this) {
        is Success -> ""
        is Error.NoFileUploaded -> "Please upload Daily Work.xlsx."
        is Error.InvalidFile    -> "The selected file is not a valid Excel workbook."
        is Error.SheetNotFound  -> "The \"Daily Work\" worksheet was not found."
        is Error.SaveFailure    -> "Unable to save changes. Please try again."
        is Error.RowNotFound    -> "Activity \"$id\" not found in Excel file."
        is Error.Generic        -> message
    }

    companion object {
        fun <T> success(data: T): ExcelResult<T> = Success(data)
        fun noFile(): ExcelResult<Nothing>        = Error.NoFileUploaded
        fun invalidFile(): ExcelResult<Nothing>   = Error.InvalidFile
        fun sheetNotFound(): ExcelResult<Nothing> = Error.SheetNotFound
        fun saveFailed(cause: Throwable? = null): ExcelResult<Nothing> = Error.SaveFailure(cause)
    }
}
