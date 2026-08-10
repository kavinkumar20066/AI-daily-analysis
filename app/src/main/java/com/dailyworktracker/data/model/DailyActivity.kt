package com.dailyworktracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room entity representing a single tracked activity.
 * Maps 1-to-1 with a row in the "Daily Work" Excel sheet (columns A–P).
 *
 * ID format: ACT-000001 (sequential, never reused, never the activity name).
 */
@Entity(tableName = "activities")
data class DailyActivity(
    /** Col A – Unique ID in ACT-000001 format */
    @PrimaryKey val id: String,

    /** Col B – Date of the activity */
    val date: LocalDate,

    /** Col C – Day name (derived from date, e.g. "Monday") */
    val day: String,

    /** Col D – Name of the activity (required) */
    val activityName: String,

    /** Col E – Category (predefined or custom) */
    val category: String,

    /** Col F – Start time (optional, e.g. "09:00") */
    val startTime: String?,

    /** Col G – End time (optional, e.g. "10:30") */
    val endTime: String?,

    /** Col H – Duration (optional, e.g. "1h 30m") */
    val duration: String?,

    /** Col I – Status: Completed | InProgress | Pending | Skipped */
    val status: ActivityStatus,

    /** Col J – Priority: High | Medium | Low */
    val priority: ActivityPriority,

    /** Col K – Free-text notes */
    val notes: String?,

    /** Col L – Whether this is an exercise activity */
    val isExercise: Boolean,

    /** Col M – Distance in km (exercise only) */
    val distance: String?,

    /** Col N – Calories burned (exercise only) */
    val calories: Int?,

    /** Col O – When the record was first created */
    val createdAt: LocalDateTime,

    /** Col P – When the record was last updated */
    val updatedAt: LocalDateTime
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE")

        /**
         * Generate the next sequential ID given the current max number.
         * e.g. generateId(0) → "ACT-000001"
         */
        fun generateId(nextNumber: Int): String =
            "ACT-%06d".format(nextNumber)

        /**
         * Parse an ID string and return its numeric part.
         * e.g. "ACT-000042" → 42
         */
        fun parseIdNumber(id: String): Int =
            id.removePrefix("ACT-").trimStart('0').toIntOrNull() ?: 0

        /**
         * Derive day name from a LocalDate.
         */
        fun dayNameFromDate(date: LocalDate): String =
            date.format(DATE_FORMATTER)

        /**
         * Create a new activity with auto-derived day and timestamps.
         */
        fun create(
            id: String,
            date: LocalDate,
            activityName: String,
            category: String,
            startTime: String? = null,
            endTime: String? = null,
            duration: String? = null,
            status: ActivityStatus = ActivityStatus.Pending,
            priority: ActivityPriority = ActivityPriority.Medium,
            notes: String? = null,
            isExercise: Boolean = false,
            distance: String? = null,
            calories: Int? = null
        ): DailyActivity {
            val now = LocalDateTime.now()
            return DailyActivity(
                id = id,
                date = date,
                day = dayNameFromDate(date),
                activityName = activityName,
                category = category,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                status = status,
                priority = priority,
                notes = notes,
                isExercise = isExercise,
                distance = distance,
                calories = calories,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    /** Returns the same activity with status updated and updatedAt refreshed. */
    fun withStatus(newStatus: ActivityStatus) = copy(
        status = newStatus,
        updatedAt = LocalDateTime.now()
    )

    /** Returns the activity with updatedAt refreshed. */
    fun asUpdated() = copy(updatedAt = LocalDateTime.now())

    /** True if the activity has useful duration information. */
    fun hasDuration(): Boolean = !duration.isNullOrBlank()

    /** True if this activity has exercise-specific data. */
    fun hasExerciseData(): Boolean = isExercise && (!distance.isNullOrBlank() || calories != null)
}
