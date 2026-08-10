package com.dailyworktracker.ui.screens.summary

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailyworktracker.ui.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(dateString: String, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel = remember { DailySummaryViewModel(application, dateString) }
    val summary by viewModel.summary.collectAsState()

    val animRate by animateFloatAsState(
        targetValue = summary.completionRate / 100f,
        animationSpec = tween(1000), label = "rate"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        summary.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Completion Rate Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Completion Rate", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    LinearProgressIndicator(
                        progress = { animRate },
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = when {
                            summary.completionRate >= 80 -> GreenCompleted
                            summary.completionRate >= 50 -> AmberInProgress
                            else -> RedPending
                        },
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    Text("${summary.completionRate.toInt()}% (${summary.completedCount}/${summary.totalCount})",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(Modifier.weight(1f), Icons.Default.Timer, "Productive",
                    summary.productiveHoursStr, CyanAccent)
                StatChip(Modifier.weight(1f), Icons.Default.FitnessCenter, "Exercise",
                    "${summary.exerciseCount} sessions", GreenCompleted)
            }

            // Category breakdown
            if (summary.categoryBreakdown.isNotEmpty()) {
                SummaryCard("Time by Category") {
                    summary.categoryBreakdown.entries.sortedByDescending { it.value }.forEach { (cat, count) ->
                        val frac = count.toFloat() / summary.totalCount
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(cat, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(90.dp),
                                color = categoryColor(cat))
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier.weight(1f).height(8.dp),
                                color = categoryColor(cat),
                                trackColor = categoryColor(cat).copy(alpha = 0.15f)
                            )
                            Text("$count", style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(24.dp))
                        }
                    }
                }
            }

            // Highlights
            if (summary.highlights.isNotEmpty()) {
                SummaryCard("Today's Highlights") {
                    summary.highlights.forEach { h ->
                        Text(h, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            // Areas to improve
            if (summary.improvements.isNotEmpty()) {
                SummaryCard("Areas to Improve",
                    containerColor = AmberInProgress.copy(alpha = 0.08f)) {
                    summary.improvements.forEach { imp ->
                        Text(imp, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String, color: Color
) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            content()
        }
    }
}
