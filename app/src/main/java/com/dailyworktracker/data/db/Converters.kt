package com.dailyworktracker.data.db

import androidx.room.TypeConverter
import com.dailyworktracker.data.model.ActivityPriority
import com.dailyworktracker.data.model.ActivityStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Room TypeConverters for non-primitive types.
 * All conversions are deterministic, lossless, and use ISO-8601 string format.
 */
class Converters {

    // ─── LocalDate ────────────────────────────────────────────────────────────

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it) }

    // ─── LocalDateTime ────────────────────────────────────────────────────────

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? = dateTime?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it) }

    // ─── ActivityStatus ───────────────────────────────────────────────────────

    @TypeConverter
    fun fromActivityStatus(status: ActivityStatus): String = status.name

    @TypeConverter
    fun toActivityStatus(value: String): ActivityStatus =
        ActivityStatus.fromString(value)

    // ─── ActivityPriority ─────────────────────────────────────────────────────

    @TypeConverter
    fun fromActivityPriority(priority: ActivityPriority): String = priority.name

    @TypeConverter
    fun toActivityPriority(value: String): ActivityPriority =
        ActivityPriority.fromString(value)
}
