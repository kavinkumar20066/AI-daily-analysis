package com.dailyworktracker.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyworktracker.appContainer
import com.dailyworktracker.notification.NotificationScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for SettingsScreen.
 * Reads/writes notification preferences and delegates scheduling to [NotificationScheduler].
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.appContainer.userPreferencesRepository
    private val ctx   = application.applicationContext

    /** Whether the daily reminder notification is enabled. */
    val notificationsEnabled: StateFlow<Boolean> = prefs.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Scheduled notification hour (0–23, 24-hour clock). */
    val notificationHour: StateFlow<Int> = prefs.notificationHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 20)

    /** Scheduled notification minute (0–59). */
    val notificationMinute: StateFlow<Int> = prefs.notificationMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Enables or disables the daily reminder.
     * If enabling, schedules via WorkManager at [hour]:[minute].
     * If disabling, cancels the WorkManager job.
     */
    fun setNotificationEnabled(enabled: Boolean, hour: Int = notificationHour.value, minute: Int = notificationMinute.value) {
        viewModelScope.launch {
            prefs.saveNotificationSettings(enabled, hour, minute)
            if (enabled) {
                NotificationScheduler.schedule(ctx, hour, minute)
            } else {
                NotificationScheduler.cancel(ctx)
            }
        }
    }

    /**
     * Updates only the scheduled time (without changing enabled state).
     * Re-schedules WorkManager if notifications are enabled.
     */
    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.saveNotificationSettings(notificationsEnabled.value, hour, minute)
            if (notificationsEnabled.value) {
                NotificationScheduler.schedule(ctx, hour, minute)
            }
        }
    }
}
