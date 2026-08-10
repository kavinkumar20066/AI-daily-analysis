package com.dailyworktracker.data.db

import androidx.paging.PagingSource
import androidx.room.*
import com.dailyworktracker.data.model.DailyActivity
import com.dailyworktracker.data.model.ActivityStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for all activity CRUD and analytics queries.
 *
 * Dates are stored/queried as ISO-8601 strings (YYYY-MM-DD) because
 * Room's SQLite layer stores them via the Converters class.
 */
@Dao
interface ActivityDao {

    // ─── INSERT / UPDATE / DELETE ─────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: DailyActivity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<DailyActivity>)

    @Update
    suspend fun updateActivity(activity: DailyActivity)

    @Delete
    suspend fun deleteActivity(activity: DailyActivity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: String)

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()

    // ─── SINGLE RECORD ────────────────────────────────────────────────────────

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    suspend fun getActivityById(id: String): DailyActivity?

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    fun observeActivityById(id: String): Flow<DailyActivity?>

    // ─── DAILY QUERIES ────────────────────────────────────────────────────────

    @Query("SELECT * FROM activities WHERE date = :date ORDER BY startTime ASC, activityName ASC")
    fun getActivitiesForDate(date: String): Flow<List<DailyActivity>>

    @Query("SELECT * FROM activities WHERE date = :date ORDER BY startTime ASC")
    suspend fun getActivitiesForDateSync(date: String): List<DailyActivity>

    @Query("SELECT COUNT(*) FROM activities WHERE date = :date")
    fun getTotalCountForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM activities WHERE date = :date AND status = 'Completed'")
    fun getCompletedCountForDate(date: String): Flow<Int>

    @Query("""
        SELECT * FROM activities 
        WHERE date = :date AND status = 'Completed'
        ORDER BY startTime ASC
    """)
    suspend fun getCompletedActivitiesForDate(date: String): List<DailyActivity>

    // ─── DATE RANGE ───────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM activities 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date ASC, startTime ASC
    """)
    fun getActivitiesForDateRange(startDate: String, endDate: String): Flow<List<DailyActivity>>

    @Query("""
        SELECT * FROM activities 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date ASC, startTime ASC
    """)
    suspend fun getActivitiesForDateRangeSync(startDate: String, endDate: String): List<DailyActivity>

    // ─── MONTHLY QUERIES ──────────────────────────────────────────────────────

    /**
     * yearMonth format: "YYYY-MM" (e.g. "2026-08")
     */
    @Query("""
        SELECT * FROM activities 
        WHERE strftime('%Y-%m', date) = :yearMonth 
        ORDER BY date ASC, startTime ASC
    """)
    fun getActivitiesForMonth(yearMonth: String): Flow<List<DailyActivity>>

    @Query("""
        SELECT * FROM activities 
        WHERE strftime('%Y-%m', date) = :yearMonth 
        ORDER BY date ASC, startTime ASC
    """)
    suspend fun getActivitiesForMonthSync(yearMonth: String): List<DailyActivity>

    @Query("""
        SELECT COUNT(*) FROM activities 
        WHERE strftime('%Y-%m', date) = :yearMonth AND status = 'Completed'
    """)
    suspend fun getCompletedCountForMonth(yearMonth: String): Int

    @Query("""
        SELECT COUNT(*) FROM activities 
        WHERE strftime('%Y-%m', date) = :yearMonth
    """)
    suspend fun getTotalCountForMonth(yearMonth: String): Int

    // ─── YEARLY QUERIES ───────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM activities 
        WHERE strftime('%Y', date) = :year 
        ORDER BY date ASC, startTime ASC
    """)
    fun getActivitiesForYear(year: String): Flow<List<DailyActivity>>

    @Query("""
        SELECT * FROM activities 
        WHERE strftime('%Y', date) = :year 
        ORDER BY date ASC
    """)
    suspend fun getActivitiesForYearSync(year: String): List<DailyActivity>

    // ─── ALL ACTIVITIES (PAGED) ───────────────────────────────────────────────

    @Query("SELECT * FROM activities ORDER BY date DESC, startTime ASC")
    fun getAllActivitiesPaged(): PagingSource<Int, DailyActivity>

    @Query("SELECT * FROM activities ORDER BY date DESC, startTime ASC")
    fun getAllActivities(): Flow<List<DailyActivity>>

    // ─── COUNTS & METADATA ────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getActivityCount(): Int

    @Query("SELECT MAX(CAST(SUBSTR(id, 5) AS INTEGER)) FROM activities")
    suspend fun getMaxActivityNumber(): Int?

    @Query("SELECT DISTINCT date FROM activities ORDER BY date DESC")
    fun getAllActivityDates(): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM activities ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    // ─── SEARCH & FILTER ──────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM activities
        WHERE activityName LIKE '%' || :query || '%'
           OR category     LIKE '%' || :query || '%'
           OR notes        LIKE '%' || :query || '%'
        ORDER BY date DESC, startTime ASC
    """)
    fun searchActivities(query: String): Flow<List<DailyActivity>>

    @Query("""
        SELECT * FROM activities
        WHERE (:query  = '' OR activityName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        AND   (:status = '' OR status   = :status)
        AND   (:category = '' OR category = :category)
        AND   (:isExerciseOnly = 0 OR isExercise = 1)
        ORDER BY date DESC, startTime ASC
    """)
    fun getFilteredActivities(
        query: String = "",
        status: String = "",
        category: String = "",
        isExerciseOnly: Int = 0
    ): Flow<List<DailyActivity>>

    // ─── ANALYTICS ────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM activities 
        WHERE isExercise = 1 
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun getRecentExerciseActivities(limit: Int = 10): Flow<List<DailyActivity>>

    @Query("""
        SELECT category, COUNT(*) as count
        FROM activities
        WHERE strftime('%Y-%m', date) = :yearMonth
        GROUP BY category
        ORDER BY count DESC
    """)
    suspend fun getCategoryCountsForMonth(yearMonth: String): List<CategoryCount>

    @Query("""
        SELECT date, COUNT(*) as total,
               SUM(CASE WHEN status = 'Completed' THEN 1 ELSE 0 END) as completed
        FROM activities
        WHERE strftime('%Y-%m', date) = :yearMonth
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyCountsForMonth(yearMonth: String): List<DailyCount>
}

/** Lightweight projection for category-level aggregation. */
data class CategoryCount(
    val category: String,
    val count: Int
)

/** Lightweight projection for day-level aggregation. */
data class DailyCount(
    val date: String,
    val total: Int,
    val completed: Int
)
