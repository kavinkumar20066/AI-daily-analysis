package com.dailyworktracker.ui.screens.excel

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.FileProvider
import com.dailyworktracker.MainActivity
import com.dailyworktracker.ui.theme.*
import com.dailyworktracker.ui.viewmodel.ExcelUiState
import com.dailyworktracker.ui.viewmodel.ExcelViewModel
import java.text.SimpleDateFormat
import java.util.*

/** Fully functional Excel File Management screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelManagementScreen(onBack: () -> Unit) {
    val context  = LocalContext.current
    val activity = context as? MainActivity
    val viewModel: ExcelViewModel = remember { activity?.excelViewModel!! }

    val uiState       by viewModel.uiState.collectAsState()
    val isConnected   by viewModel.isFileConnected.collectAsState()
    val lastUpdated   by viewModel.lastUpdated.collectAsState()
    val fileName      by viewModel.connectedFileName.collectAsState()

    var showDisconnectDialog by remember { mutableStateOf(false) }
    var snackMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excel File Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Connection Status Card ────────────────────────────────────────
            ConnectionStatusCard(
                isConnected = isConnected,
                fileName    = fileName,
                lastUpdated = lastUpdated
            )

            // ── Loading Indicator ─────────────────────────────────────────────
            AnimatedVisibility(visible = uiState is ExcelUiState.Loading) {
                val msg = (uiState as? ExcelUiState.Loading)?.message ?: "Working…"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(msg, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Error Banner ──────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState is ExcelUiState.Error) {
                val errMsg = (uiState as? ExcelUiState.Error)?.message ?: ""
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                        Text(errMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                    }
                }
            }

            // ── Action Buttons ────────────────────────────────────────────────
            Text(
                "File Actions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isConnected) {
                // No file connected — show upload button prominently
                Button(
                    onClick = { activity?.launchExcelPicker() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Daily Work.xlsx", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                // Connected — show full action grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExcelActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FileUpload,
                        label = "Replace File",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { activity?.launchExcelPicker() }
                    )
                    ExcelActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Refresh,
                        label = "Reload",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = { viewModel.reload() }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExcelActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Save,
                        label = "Save All",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = { viewModel.saveAll() }
                    )
                    ExcelActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Backup,
                        label = "Backup",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            viewModel.createBackup { result ->
                                result.getOrNull()?.let { file ->
                                    // Share the backup file
                                    val fileUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        putExtra(Intent.EXTRA_STREAM, fileUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Save Backup"))
                                    snackMessage = "Backup created: ${file.name}"
                                } ?: run {
                                    snackMessage = "Backup failed"
                                }
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExcelActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Share,
                        label = "Share",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            viewModel.createBackup { result ->
                                result.getOrNull()?.let { file ->
                                    val fileUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        putExtra(Intent.EXTRA_STREAM, fileUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Excel"))
                                }
                            }
                        }
                    )
                    ExcelActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LinkOff,
                        label = "Disconnect",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = { showDisconnectDialog = true }
                    )
                }
            }

            // ── Info Section ──────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Text("How it works", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "• Your Excel file (Daily Work.xlsx) is the permanent source of truth.\n" +
                        "• All activities are stored in the \"Daily Work\" sheet (columns A–P).\n" +
                        "• Add/Edit/Delete operations update both the app and your Excel file in real time.\n" +
                        "• The app cache (Room) lets you work fully offline.\n" +
                        "• Data is never lost — the app never overwrites without reading first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Disconnect confirmation dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect File?") },
            text  = {
                Text("Your app data will be preserved. You can reconnect or upload a new file at any time.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.disconnect()
                    showDisconnectDialog = false
                    onBack()
                }) { Text("Disconnect", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    fileName: String?,
    lastUpdated: Long
) {
    val color = if (isConnected) GreenCompleted else GraySkipped
    val bgColor = if (isConnected)
        GreenCompleted.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isConnected) "Excel File Connected" else "No Excel File Connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                if (isConnected && fileName != null) {
                    Text(
                        fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isConnected && lastUpdated > 0) {
                    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                    Text(
                        "Last sync: ${sdf.format(Date(lastUpdated))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExcelActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}
