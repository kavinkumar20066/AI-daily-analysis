package com.dailyworktracker.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyworktracker.MainActivity
import com.dailyworktracker.ui.viewmodel.ExcelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onExcelManagement: () -> Unit
) {
    val context  = LocalContext.current
    val activity = context as? MainActivity
    val excelVm  = activity?.excelViewModel

    val isConnected by (excelVm?.isFileConnected
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val fileName by (excelVm?.connectedFileName
        ?: kotlinx.coroutines.flow.MutableStateFlow<String?>(null)).collectAsState()

    // Settings ViewModel for notification prefs
    val settingsVm: SettingsViewModel = viewModel()
    val notificationsEnabled by settingsVm.notificationsEnabled.collectAsState()
    val notifHour            by settingsVm.notificationHour.collectAsState()
    val notifMinute          by settingsVm.notificationMinute.collectAsState()

    // Time-picker dialog state
    var showTimePicker by remember { mutableStateOf(false) }

    // Rationale dialog state (shown before launching the system permission prompt on API 33+)
    var showPermissionRationale by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Excel File Section ─────────────────────────────────────────────
            SettingsSectionHeader("Excel File")

            SettingsItem(
                icon     = Icons.Default.TableChart,
                title    = "Excel File Management",
                subtitle = if (isConnected) "Connected: ${fileName ?: "Daily Work.xlsx"}"
                           else "No file connected — tap to upload",
                onClick  = onExcelManagement,
                badge    = if (isConnected) "CONNECTED" else null,
                badgeIsGreen = isConnected
            )

            SettingsItem(
                icon     = Icons.Default.Refresh,
                title    = "Reload from Excel",
                subtitle = "Re-sync Room cache from the Excel file",
                onClick  = { excelVm?.reload() }
            )

            SettingsItem(
                icon     = Icons.Default.Save,
                title    = "Save All to Excel",
                subtitle = "Write all activities back to Excel",
                onClick  = { excelVm?.saveAll() }
            )

            // ── Data Section ──────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Data")

            SettingsItem(
                icon     = Icons.Default.Backup,
                title    = "Create Backup",
                subtitle = "Save a timestamped copy of your Excel file",
                onClick  = {
                    excelVm?.createBackup { result ->
                        result.getOrNull()?.let { file ->
                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.provider", file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Save Backup"))
                        }
                    }
                }
            )

            // ── Notifications Section ─────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Notifications")

            // Toggle card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Daily Reminder",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (notificationsEnabled)
                                "Reminds you every day at ${formatTime(notifHour, notifMinute)}"
                            else
                                "Tap to enable a daily logging reminder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked         = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                // On Android 13+, check POST_NOTIFICATIONS permission before enabling
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val permissionGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (permissionGranted) {
                                        // Permission already granted — enable directly
                                        settingsVm.setNotificationEnabled(true, notifHour, notifMinute)
                                    } else {
                                        // Show rationale dialog first, then request permission
                                        showPermissionRationale = true
                                    }
                                } else {
                                    // API < 33 — no runtime permission needed
                                    settingsVm.setNotificationEnabled(true, notifHour, notifMinute)
                                }
                            } else {
                                settingsVm.setNotificationEnabled(false, notifHour, notifMinute)
                            }
                        }
                    )
                }
            }

            // Time picker row — only visible when notifications are enabled
            AnimatedVisibility(
                visible = notificationsEnabled,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                SettingsItem(
                    icon     = Icons.Default.Schedule,
                    title    = "Reminder Time",
                    subtitle = "Currently set to ${formatTime(notifHour, notifMinute)}",
                    onClick  = { showTimePicker = true }
                )
            }

            // ── App Section ───────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("App")

            SettingsItem(
                icon     = Icons.Default.Info,
                title    = "About",
                subtitle = "Daily Work Tracker v1.0 · Track. Improve. Grow.",
                onClick  = {}
            )

            Spacer(Modifier.height(32.dp))

            // App version footer
            Text(
                "Daily Work Tracker 1.0.0\nBuilt with Kotlin + Jetpack Compose",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // ── Time Picker Dialog ─────────────────────────────────────────────────────
    if (showTimePicker) {
        NotificationTimePickerDialog(
            initialHour   = notifHour,
            initialMinute = notifMinute,
            onConfirm     = { h, m ->
                settingsVm.setNotificationTime(h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // ── Notification Permission Rationale Dialog (Android 13+) ─────────────────
    if (showPermissionRationale) {
        val activity = context as? MainActivity
        NotificationPermissionRationaleDialog(
            onConfirm = {
                showPermissionRationale = false
                // Launch the system permission prompt via the Activity launcher
                activity?.requestNotificationPermission { granted ->
                    if (granted) {
                        settingsVm.setNotificationEnabled(true, notifHour, notifMinute)
                    }
                    // If denied, leave the toggle off (no action needed)
                }
            },
            onDismiss = {
                showPermissionRationale = false
                // User dismissed — do not enable notifications
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Time Picker Dialog (Material3 TimeInput)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTimePickerDialog(
    initialHour:   Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour    = initialHour,
        initialMinute  = initialMinute,
        is24Hour       = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Set")
            }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                "Set Reminder Time",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Notification Permission Rationale Dialog (Android 13+)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Explains why POST_NOTIFICATIONS permission is needed, then lets the user
 * proceed to the system prompt (onConfirm) or cancel (onDismiss).
 */
@Composable
private fun NotificationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Enable Notifications",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "To send you daily reminders to log your activities, the app needs " +
                "permission to post notifications.\n\n" +
                "Tap \"Allow\" to grant the permission in the next system dialog. " +
                "You can change this at any time in your device Settings.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables
// ─────────────────────────────────────────────────────────────────────────────

/** Formats an hour (0–23) and minute (0–59) into a 12-hour display string. */
private fun formatTime(hour: Int, minute: Int): String {
    val h12    = if (hour % 12 == 0) 12 else hour % 12
    val suffix = if (hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, minute, suffix)
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon:         ImageVector,
    title:        String,
    subtitle:     String,
    onClick:      () -> Unit,
    badge:        String?  = null,
    badgeIsGreen: Boolean  = false
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge != null) {
                val badgeColor = if (badgeIsGreen)
                    com.dailyworktracker.ui.theme.GreenCompleted
                else
                    MaterialTheme.colorScheme.error
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        badge,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
