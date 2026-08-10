package com.dailyworktracker.ui.screens.monthly

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyworktracker.ui.theme.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyProgressScreen(yearMonth: String, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel   = remember { MonthlyViewModel(application, yearMonth) }
    val data        by viewModel.data.collectAsState()

    val title = try {
        YearMonth.parse(yearMonth).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    } catch (_: Exception) { yearMonth }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back") } })
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
            // Summary Cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "${data.completionRate.toInt()}%",
                    "Completion", VioletPrimary)
                StatCard(Modifier.weight(1f), "%.1fh".format(data.productiveHours),
                    "Productive", CyanAccent)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "${data.productiveDays}",
                    "Active Days", GreenCompleted)
                StatCard(Modifier.weight(1f), "%.1f".format(data.avgActivitiesPerDay),
                    "Avg/Day", AmberInProgress)
            }

            // Activities Chart (daily bar)
            if (data.dailyCounts.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Daily Activity Count", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        val maxTotal = data.dailyCounts.maxOfOrNull { it.second.first } ?: 1
                        Row(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            data.dailyCounts.forEach { (dateStr, counts) ->
                                val (total, completed) = counts
                                val fraction = total.toFloat() / maxTotal
                                val dayNum = try { dateStr.substring(8).toInt().toString() }
                                    catch (_: Exception) { "" }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Box(modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                        .background(
                                            if (completed == total && total > 0) GreenCompleted
                                            else if (completed > 0) AmberInProgress
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }
                        Text("Each bar = one day  |  Green = all done  |  Amber = partial",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Category breakdown
            if (data.categoryBreakdown.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Category Distribution", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        val total = data.categoryBreakdown.values.sum()
                        data.categoryBreakdown.entries.sortedByDescending { it.value }.forEach { (cat, count) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp))
                                    .background(categoryColor(cat)))
                                Text(cat, Modifier.width(90.dp),
                                    style = MaterialTheme.typography.bodySmall)
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / total },
                                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = categoryColor(cat),
                                    trackColor = categoryColor(cat).copy(alpha = 0.15f)
                                )
                                Text("$count", Modifier.width(24.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }

            // Exercise stats
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GreenCompleted.copy(alpha = 0.08f))) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FitnessCenter, null, tint = GreenCompleted,
                            modifier = Modifier.size(24.dp))
                        Text("${data.exerciseDays}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = GreenCompleted)
                        Text("Exercise Days", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = VioletPrimary,
                            modifier = Modifier.size(24.dp))
                        Text("${data.completedActivities}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = VioletPrimary)
                        Text("Completed", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Assignment, null, tint = CyanAccent,
                            modifier = Modifier.size(24.dp))
                        Text("${data.totalActivities}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = CyanAccent)
                        Text("Total", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, color: Color) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
