package com.dailyworktracker.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyworktracker.MainActivity
import com.dailyworktracker.ui.viewmodel.ExcelViewModel
import com.dailyworktracker.ui.viewmodel.ExcelUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onExcelManagement: () -> Unit
) {
    val context   = LocalContext.current
    val activity  = context as? MainActivity
    val excelVm   = activity?.excelViewModel

    val isConnected by (excelVm?.isFileConnected ?: kotlinx.coroutines.flow.MutableStateFlow(false))
        .collectAsState()
    val fileName by (excelVm?.connectedFileName ?: kotlinx.coroutines.flow.MutableStateFlow<String?>(null))
        .collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge) }
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
                icon       = Icons.Default.TableChart,
                title      = "Excel File Management",
                subtitle   = if (isConnected) "Connected: ${fileName ?: "Daily Work.xlsx"}"
                             else "No file connected — tap to upload",
                onClick    = onExcelManagement,
                badge      = if (isConnected) "CONNECTED" else null,
                badgeIsGreen = isConnected
            )

            SettingsItem(
                icon    = Icons.Default.Refresh,
                title   = "Reload from Excel",
                subtitle = "Re-sync Room cache from the Excel file",
                onClick  = { excelVm?.reload() }
            )

            SettingsItem(
                icon    = Icons.Default.Save,
                title   = "Save All to Excel",
                subtitle = "Write all activities back to Excel",
                onClick  = { excelVm?.saveAll() }
            )

            // ── Data Section ──────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("Data")

            SettingsItem(
                icon    = Icons.Default.Backup,
                title   = "Create Backup",
                subtitle = "Save a timestamped copy of your Excel file",
                onClick  = {
                    excelVm?.createBackup { result ->
                        result.getOrNull()?.let { file ->
                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.provider", file)
                            val intent  = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Save Backup"))
                        }
                    }
                }
            )

            // ── App Section ───────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            SettingsSectionHeader("App")

            SettingsItem(
                icon    = Icons.Default.Notifications,
                title   = "Notification Settings",
                subtitle = "Configure daily reminder notifications",
                onClick  = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )

            SettingsItem(
                icon    = Icons.Default.Info,
                title   = "About",
                subtitle = "Daily Work Tracker v1.0 · Track. Improve. Grow.",
                onClick  = {}
            )

            Spacer(Modifier.height(32.dp))

            // App version footer
            Text(
                "Daily Work Tracker 1.0.0\nBuilt with Kotlin + Jetpack Compose",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badge: String? = null,
    badgeIsGreen: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
