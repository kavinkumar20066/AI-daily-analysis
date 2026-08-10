package com.dailyworktracker.ui.screens.yearly

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyProgressScreen(year: String, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel   = remember { YearlyViewModel(application, year) }
    val data        by viewModel.data.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Year $year", fontWeight = FontWeight.Bold) },
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
            // Year summary cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                YearStatCard(Modifier.weight(1f), "${data.yearlyRate.toInt()}%",
                    "Yearly Completion", VioletPrimary)
                YearStatCard(Modifier.weight(1f), "%.0fh".format(data.totalProductiveHours),
                    "Total Productive", CyanAccent)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                YearStatCard(Modifier.weight(1f), "${data.totalActivities}",
                    "Total Activities", AmberInProgress)
                YearStatCard(Modifier.weight(1f), "${data.totalCompleted}",
                    "Total Completed", GreenCompleted)
            }

            // Best month
            if (data.mostProductiveMonth != "-") {
                Card(colors = CardDefaults.cardColors(
                    containerColor = GreenCompleted.copy(alpha = 0.12f))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.EmojiEvents, null, tint = GreenCompleted,
                            modifier = Modifier.size(32.dp))
                        Column {
                            Text("Most Productive Month",
                                style = MaterialTheme.typography.labelMedium,
                                color = GreenCompleted)
                            Text("${data.mostProductiveMonth} (${data.mostProductiveRate.toInt()}%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Monthly bar chart
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Monthly Completion %", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        data.monthStats.forEach { month ->
                            val fraction = month.rate / 100f
                            val barColor = when {
                                month.rate >= 80 -> GreenCompleted
                                month.rate >= 40 -> AmberInProgress
                                month.total == 0 -> Color.Transparent
                                else -> RedPending
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (month.total > 0) {
                                    Text("${month.rate.toInt()}",
                                        fontSize = 7.sp,
                                        color = barColor,
                                        fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(fraction.coerceAtLeast(
                                            if (month.total == 0) 0f else 0.02f
                                        ))
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(
                                            if (month.total == 0)
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            else barColor
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(month.label, fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // Month table
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Monthly Breakdown", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)

                    // Header
                    Row(Modifier.fillMaxWidth()) {
                        Text("Month", Modifier.weight(2f),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Total", Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Done", Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Rate", Modifier.weight(1f), textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()

                    data.monthStats.filter { it.total > 0 }.forEach { month ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(month.label, Modifier.weight(2f),
                                style = MaterialTheme.typography.bodySmall)
                            Text("${month.total}", Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall)
                            Text("${month.completed}", Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = GreenCompleted)
                            Text("${month.rate.toInt()}%", Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    month.rate >= 80 -> GreenCompleted
                                    month.rate >= 40 -> AmberInProgress
                                    else -> RedPending
                                })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearStatCard(modifier: Modifier, value: String, label: String, color: Color) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}
