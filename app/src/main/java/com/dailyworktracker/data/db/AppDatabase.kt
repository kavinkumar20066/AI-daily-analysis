package com.dailyworktracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dailyworktracker.data.model.DailyActivity

/**
 * Room database — local cache only.
 * Source of truth is always Daily Work.xlsx.
 *
 * Version history:
 * 1 – Initial schema with 'activities' table (all 16 spec columns).
 */
@Database(
    entities = [DailyActivity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao

    companion object {
        private const val DATABASE_NAME = "daily_work_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Allow main-thread queries ONLY during development.
                // All production queries must be on IO dispatcher.
                .fallbackToDestructiveMigration()
                .build()
    }
}
