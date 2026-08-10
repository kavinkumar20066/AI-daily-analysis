package com.dailyworktracker.ui.screens.progress

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailyworktracker.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Progress hub — quick access to Weekly, Monthly, Yearly screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    contentPadding: PaddingValues,
    onWeeklyClick: (String) -> Unit,
    onMonthlyClick: (String) -> Unit,
    onYearlyClick: (String) -> Unit
) {
    val today = LocalDate.now()
    val thisMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Progress", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge)
            })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Explore Your Progress",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text("Dive into your productivity analytics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            ProgressCard(
                icon        = Icons.Default.DateRange,
                title       = "Weekly View",
                subtitle    = "Week of ${thisMonday.format(DateTimeFormatter.ofPattern("dd MMM"))}",
                description = "Per-day completion %, productive hours, and exercise summary for the current week",
                color       = CyanAccent,
                onClick     = { onWeeklyClick(thisMonday.toString()) }
            )

            ProgressCard(
                icon        = Icons.Default.CalendarMonth,
                title       = "Monthly Progress",
                subtitle    = today.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                description = "Totals, completion rate, category breakdown, productive days, and charts for the current month",
                color       = VioletPrimary,
                onClick     = {
                    onMonthlyClick(today.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                }
            )

            ProgressCard(
                icon        = Icons.Default.BarChart,
                title       = "Yearly Overview",
                subtitle    = today.year.toString(),
                description = "Month-by-month breakdown, yearly totals, most productive periods, and trend charts",
                color       = GreenCompleted,
                onClick     = { onYearlyClick(today.year.toString()) }
            )

            // Previous periods
            HorizontalDivider()
            Text("Previous Periods",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            val lastMonth = YearMonth.now().minusMonths(1)
            val lastYear  = today.year - 1

            OutlinedCard(
                onClick = {
                    onMonthlyClick(lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(lastMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            OutlinedCard(
                onClick = { onYearlyClick(lastYear.toString()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Year $lastYear", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null,
                        tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null,
                tint = color, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
