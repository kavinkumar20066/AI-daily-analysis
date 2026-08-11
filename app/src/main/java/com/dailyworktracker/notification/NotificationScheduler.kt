package com.dailyworktracker.notification

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Utility object that manages WorkManager scheduling for daily reminder notifications.
 *
 * Usage:
 * - [schedule]: Creates (or replaces) a periodic daily reminder at the given hour/minute.
 * - [cancel]:   Removes the scheduled periodic work.
 */
object NotificationScheduler {

    /**
     * Schedules (or replaces) a daily reminder notification at [hour]:[minute] each day.
     *
     * WorkManager calculates the initial delay so the first trigger is at the correct wall-clock
     * time, then repeats every 24 hours.
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val initialDelay = calculateInitialDelay(hour, minute)

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            repeatInterval       = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,  // Replaces existing if schedule changes
            request
        )
    }

    /** Cancels the scheduled daily reminder. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }

    /**
     * Returns the milliseconds from now until the next occurrence of [hour]:[minute].
     * If that time has already passed today, schedules for tomorrow.
     */
    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the target time has already passed today, schedule for tomorrow
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
