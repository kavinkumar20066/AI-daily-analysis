package com.dailyworktracker.ui.screens.weekly

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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScreen(startDateString: String, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel   = remember { WeeklyViewModel(application, startDateString) }
    val data        by viewModel.weeklyData.collectAsState()

    val dayFmt = DateTimeFormatter.ofPattern("EEE\ndd")
    val rangeFmt = DateTimeFormatter.ofPattern("dd MMM")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${viewModel.startDate.format(rangeFmt)} – ${viewModel.endDate.format(rangeFmt)}",
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
            // ── Summary Cards ─────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeekStatCard(Modifier.weight(1f), "${data.weeklyRate.toInt()}%",
                    "Completion", VioletPrimary)
                WeekStatCard(Modifier.weight(1f), "%.1fh".format(data.totalProductiveHours),
                    "Productive", CyanAccent)
                WeekStatCard(Modifier.weight(1f), "${data.totalExerciseSessions}",
                    "Exercise", GreenCompleted)
            }

            // ── Bar Chart (Completion % per day) ──────────────────────────────
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Daily Completion %", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        data.weekDays.forEach { day ->
                            val fraction = day.completionRate / 100f
                            val barColor = when {
                                day.completionRate >= 80 -> GreenCompleted
                                day.completionRate >= 40 -> AmberInProgress
                                day.activities.isEmpty() -> Color.Transparent
                                else -> RedPending
                            }
                            val isToday = day.date == java.time.LocalDate.now()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (day.activities.isNotEmpty()) {
                                    Text("${day.completionRate.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = barColor, fontSize = 9.sp)
                                }
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .fillMaxHeight(maxOf(fraction, 0.02f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (day.activities.isEmpty())
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            else barColor
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    day.date.format(dayFmt),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── Per-Day Breakdown ─────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daily Breakdown", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    data.weekDays.forEach { day ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(day.date.format(DateTimeFormatter.ofPattern("EEE")),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(32.dp))
                            LinearProgressIndicator(
                                progress = { day.completionRate / 100f },
                                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = when {
                                    day.completionRate >= 80 -> GreenCompleted
                                    day.completionRate >= 40 -> AmberInProgress
                                    else -> RedPending
                                },
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text("${day.activities.size} acts",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(48.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStatCard(modifier: Modifier, value: String, label: String, color: Color) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
