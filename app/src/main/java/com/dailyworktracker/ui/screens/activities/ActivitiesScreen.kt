package com.dailyworktracker.ui.screens.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyworktracker.ui.components.ActivityCard
import com.dailyworktracker.ui.components.DeleteActivityDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Full Activities tab — date picker + paginated list with CRUD. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    contentPadding: PaddingValues,
    onAddActivity: () -> Unit,
    onActivityClick: (String) -> Unit
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: ActivitiesViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(cls: Class<T>): T =
                ActivitiesViewModel(application) as T
        }
    )

    val activities    by viewModel.activities.collectAsState()
    val selectedDate  by viewModel.selectedDate.collectAsState()
    val totalCount    by viewModel.totalCount.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()
    val errorMessage  by viewModel.errorMessage.collectAsState()

    val snackState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackState.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Activities", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text(
                            "${completedCount}/${totalCount} completed today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivity) {
                Icon(Icons.Default.Add, contentDescription = "Add Activity")
            }
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Date Chip Row ──────────────────────────────────────────────────
            DateNavigationBar(
                selectedDate = selectedDate,
                onDateChange = viewModel::onDateChange
            )

            HorizontalDivider()

            // ── Activity List ──────────────────────────────────────────────────
            if (activities.isEmpty()) {
                EmptyActivitiesState(
                    date = selectedDate,
                    onAdd = onAddActivity,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = contentPadding.calculateBottomPadding() + 80.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = activities,
                        key = { it.id }
                    ) { activity ->
                        ActivityCard(
                            activity = activity,
                            onClick  = { onActivityClick(activity.id) },
                            onStatusToggle = { newStatus ->
                                viewModel.toggleStatus(activity, newStatus)
                            },
                            onDelete = { viewModel.requestDelete(activity) }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    pendingDelete?.let { activity ->
        DeleteActivityDialog(
            activityName = activity.activityName,
            onConfirm    = { viewModel.confirmDelete() },
            onDismiss    = { viewModel.cancelDelete() }
        )
    }
}

@Composable
private fun DateNavigationBar(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val displayFmt = DateTimeFormatter.ofPattern("EEE, dd MMM")
    val today = LocalDate.now()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onDateChange(selectedDate.minusDays(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                selectedDate.format(displayFmt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (selectedDate == today) {
                Text("Today", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedDate != today) {
                TextButton(onClick = { onDateChange(today) }) {
                    Text("Today", style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = { onDateChange(selectedDate.plusDays(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
            }
        }
    }
}

@Composable
private fun EmptyActivitiesState(
    date: LocalDate,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EventNote,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No activities for\n${date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM"))}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Activity")
        }
    }
}
