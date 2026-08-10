package com.dailyworktracker.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyworktracker.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Calendar / History screen — month grid color-coded by productivity. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    contentPadding: PaddingValues,
    onDayClick: (String) -> Unit
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: CalendarViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(cls: Class<T>): T =
                CalendarViewModel(application) as T
        }
    )

    val currentMonth by viewModel.currentMonth.collectAsState()
    val calendarDays by viewModel.calendarDays.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(contentPadding)
        ) {
            // ── Month Navigation ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }

            // ── Day Headers ───────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Calendar Grid ─────────────────────────────────────────────────
            calendarDays.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    week.forEach { day ->
                        CalendarDayCell(
                            day = day,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onDayClick(day.date.toString())
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Legend ────────────────────────────────────────────────────────
            ProductivityLegend()
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when (day.productivityLevel) {
        CalendarDay.ProductivityLevel.HIGH   -> GreenCompleted.copy(alpha = 0.3f)
        CalendarDay.ProductivityLevel.MEDIUM -> AmberInProgress.copy(alpha = 0.25f)
        CalendarDay.ProductivityLevel.LOW    -> RedPending.copy(alpha = 0.2f)
        CalendarDay.ProductivityLevel.NONE   -> Color.Transparent
    }
    val textColor = if (day.isCurrentMonth)
        MaterialTheme.colorScheme.onSurface
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (day.isToday) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .clickable(enabled = day.isCurrentMonth) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.isToday) MaterialTheme.colorScheme.primary else textColor,
                fontSize = 12.sp
            )
            if (day.total > 0 && day.isCurrentMonth) {
                Text(
                    "${day.completed}/${day.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun ProductivityLegend() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(GreenCompleted.copy(alpha = 0.3f), "≥80%  High")
            LegendItem(AmberInProgress.copy(alpha = 0.25f), "40–79%  Mid")
            LegendItem(RedPending.copy(alpha = 0.2f), "<40%  Low")
            LegendItem(Color.Transparent, "No data", bordered = true)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, bordered: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .then(if (bordered) Modifier.border(
                    1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp)
                ) else Modifier)
        )
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
