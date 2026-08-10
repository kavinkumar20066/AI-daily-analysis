package com.dailyworktracker.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyworktracker.MainActivity
import com.dailyworktracker.ui.components.ActivityCard
import com.dailyworktracker.ui.components.DeleteActivityDialog
import com.dailyworktracker.ui.theme.*
import com.dailyworktracker.ui.viewmodel.ExcelUiState

/** Home / Dashboard screen — fully wired to real data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onAddActivity: () -> Unit,
    onActivityClick: (String) -> Unit,
    onViewAll: () -> Unit
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(cls: Class<T>): T =
                HomeViewModel(application) as T
        }
    )

    val activity  = LocalContext.current as? MainActivity
    val excelVm   = activity?.excelViewModel
    val excelState by (excelVm?.uiState ?: kotlinx.coroutines.flow.MutableStateFlow(ExcelUiState.Idle))
        .collectAsState()
    val isConnected by (excelVm?.isFileConnected ?: kotlinx.coroutines.flow.MutableStateFlow(false))
        .collectAsState()

    val todayActivities by viewModel.todayActivities.collectAsState()
    val stats           by viewModel.stats.collectAsState()
    val pendingDelete   by viewModel.pendingDelete.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "${viewModel.greeting()} 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            viewModel.formattedDate(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddActivity,
                icon    = { Icon(Icons.Default.Add, contentDescription = null) },
                text    = { Text("Add Activity") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top    = innerPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
                start  = 16.dp,
                end    = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Excel connection banner ────────────────────────────────────────
            if (!isConnected) {
                item {
                    ExcelConnectBanner(
                        onConnect = { activity?.launchExcelPicker() }
                    )
                }
            }

            // ── Loading indicator ──────────────────────────────────────────────
            if (excelState is ExcelUiState.Loading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            // ── Today's Progress Card ─────────────────────────────────────────
            item {
                TodayProgressCard(stats = stats)
            }

            // ── Quick Stats Row ────────────────────────────────────────────────
            item {
                QuickStatsRow(stats = stats)
            }

            // ── Category Breakdown ────────────────────────────────────────────
            if (stats.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryBreakdownCard(breakdown = stats.categoryBreakdown)
                }
            }

            // ── Today's Activities ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Today's Activities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onViewAll) { Text("View All") }
                }
            }

            if (todayActivities.isEmpty()) {
                item {
                    EmptyTodayCard(onAdd = onAddActivity)
                }
            } else {
                items(
                    items = todayActivities,
                    key   = { it.id }
                ) { act ->
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

// ─── Sub-Composables ──────────────────────────────────────────────────────────

@Composable
private fun ExcelConnectBanner(onConnect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = AmberInProgress.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.TableChart, contentDescription = null,
                tint = AmberInProgress, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Connect your Excel file",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmberInProgress)
                Text("Upload Daily Work.xlsx to sync your data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onConnect) { Text("Upload") }
        }
    }
}

@Composable
private fun TodayProgressCard(stats: DashboardStats) {
    // Animated completion rate
    val animProgress by animateFloatAsState(
        targetValue = stats.completionRate / 100f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("Today's Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular progress
                CircularProgressCard(
                    progress = animProgress,
                    rate     = stats.completionRate
                )

                // Breakdown
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 24.dp)
                ) {
                    StatRow("Total",      stats.total.toString(),     MaterialTheme.colorScheme.onPrimaryContainer)
                    StatRow("Completed",  stats.completed.toString(), GreenCompleted)
                    StatRow("In Progress",stats.inProgress.toString(),AmberInProgress)
                    StatRow("Pending",    stats.pending.toString(),   RedPending)
                    if (stats.skipped > 0)
                        StatRow("Skipped", stats.skipped.toString(),  GraySkipped)
                    if (stats.productiveHours > 0)
                        StatRow("Productive",
                            "%.1fh".format(stats.productiveHours), CyanAccent)
                }
            }
        }
    }
}

@Composable
private fun CircularProgressCard(progress: Float, rate: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(100.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
            val stroke = Stroke(width = 10f, cap = StrokeCap.Round)
            // Background track
            drawArc(
                color       = Color.White.copy(alpha = 0.2f),
                startAngle  = -90f,
                sweepAngle  = 360f,
                useCenter   = false,
                style       = stroke
            )
            // Progress arc
            drawArc(
                brush      = Brush.sweepGradient(
                    listOf(VioletPrimary, CyanAccent)
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter  = false,
                style      = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${rate.toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text("done",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color))
        Text(label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        Spacer(Modifier.weight(1f))
        Text(value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color)
    }
}

@Composable
private fun QuickStatsRow(stats: DashboardStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon     = Icons.Default.CheckCircle,
            label    = "Completed",
            value    = stats.completed.toString(),
            color    = GreenCompleted
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon     = Icons.Default.Pending,
            label    = "Pending",
            value    = stats.pending.toString(),
            color    = AmberInProgress
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon     = Icons.Default.Timer,
            label    = "Prod. Hours",
            value    = "%.1f".format(stats.productiveHours),
            color    = CyanAccent
        )
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null,
                tint = color, modifier = Modifier.size(20.dp))
            Text(value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = color)
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("By Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)

            breakdown.entries
                .sortedByDescending { it.value }
                .forEach { (cat, count) ->
                    val total = breakdown.values.sum()
                    val fraction = if (total > 0) count.toFloat() / total else 0f
                    val color = categoryColor(cat)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat,
                                style = MaterialTheme.typography.bodySmall,
                                color = color, fontWeight = FontWeight.Medium)
                            Text("$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = color.copy(alpha = 0.15f)
                        )
                    }
                }
        }
    }
}

@Composable
private fun EmptyTodayCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🚀", fontSize = 48.sp)
            Text(
                "No activities yet today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Start tracking to see your progress!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add First Activity")
            }
        }
    }
}
