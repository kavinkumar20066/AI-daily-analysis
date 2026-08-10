package com.dailyworktracker

import android.content.Context
import com.dailyworktracker.data.db.AppDatabase
import com.dailyworktracker.data.excel.ExcelManager
import com.dailyworktracker.data.preferences.UserPreferencesRepository
import com.dailyworktracker.data.repository.ActivityRepository

/**
 * Manual dependency injection container.
 * Singletons are created lazily and shared across the app.
 * Access via [Context.appContainer] extension.
 */
class AppContainer(context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    val excelManager: ExcelManager by lazy {
        ExcelManager(context)
    }

    val activityRepository: ActivityRepository by lazy {
        ActivityRepository(
            context       = context,
            activityDao   = database.activityDao(),
            excelManager  = excelManager,
            prefsRepo     = userPreferencesRepository
        )
    }
}

/** App-level shortcut to the DI container. */
val Context.appContainer: AppContainer
    get() = (applicationContext as DailyWorkTrackerApp).container
