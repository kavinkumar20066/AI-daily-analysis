package com.dailyworktracker.ui.screens.daily

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
import com.dailyworktracker.ui.components.ActivityCard
import com.dailyworktracker.ui.components.DeleteActivityDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyActivitiesScreen(
    dateString: String,
    onAddActivity: () -> Unit,
    onActivityClick: (String) -> Unit,
    onBack: () -> Unit,
    onSummaryClick: () -> Unit
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel = remember {
        DailyActivitiesViewModel(application, dateString)
    }

    val activities    by viewModel.activities.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()

    val dateFmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(viewModel.date.format(dateFmt),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("${completedCount}/${activities.size} completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSummaryClick) {
                        Icon(Icons.Default.Analytics, contentDescription = "Summary")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivity) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.EventNote, contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text("No activities for this day",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center)
                    Button(onClick = onAddActivity) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Activity")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = 96.dp, start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = activities, key = { it.id }) { act ->
                    ActivityCard(
                        activity       = act,
                        onClick        = { onActivityClick(act.id) },
                        onStatusToggle = { ns -> viewModel.toggleStatus(act, ns) },
                        onDelete       = { viewModel.requestDelete(act) }
                    )
                }
            }
        }
    }

    pendingDelete?.let { act ->
        DeleteActivityDialog(
            activityName = act.activityName,
            onConfirm    = { viewModel.confirmDelete() },
            onDismiss    = { viewModel.cancelDelete() }
        )
    }
}
