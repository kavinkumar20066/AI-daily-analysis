package com.dailyworktracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailyworktracker.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that re-schedules the daily notification after a device reboot.
 *
 * WorkManager periodic work survives reboots on its own in modern versions, but registering
 * this receiver provides an extra guarantee for devices/OEMs that kill WorkManager on boot.
 *
 * Registered in AndroidManifest.xml with RECEIVE_BOOT_COMPLETED permission.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Use goAsync to safely launch a coroutine from a BroadcastReceiver
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.appContainer.userPreferencesRepository

                val enabled = prefs.notificationsEnabled.first()
                if (enabled) {
                    val hour   = prefs.notificationHour.first()
                    val minute = prefs.notificationMinute.first()
                    NotificationScheduler.schedule(context, hour, minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
