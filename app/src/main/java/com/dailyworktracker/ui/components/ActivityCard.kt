package com.dailyworktracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.DailyActivity
import com.dailyworktracker.ui.theme.*
import java.time.format.DateTimeFormatter

/**
 * Reusable activity card used in Home, DailyActivities, and ActivitiesScreen.
 * Animates in with a fade + upward slide on first composition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCard(
    activity: DailyActivity,
    onClick: () -> Unit,
    onStatusToggle: (ActivityStatus) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = false
) {
    val statusColor = statusColor(activity.status.name)
    val catColor    = categoryColor(activity.category)
    val priorityC   = priorityColor(activity.priority.name)

    // Entrance animation: fade in + slide up from 24dp below
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(activity.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(250)) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) { it / 4 },
        exit    = fadeOut(tween(150))
    ) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

            // Left status accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .background(
                        statusColor,
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Name + Priority badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        activity.activityName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    // Priority badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityC.copy(alpha = 0.15f)
                    ) {
                        Text(
                            activity.priority.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityC,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Row 2: Category + exercise icon + date (if shown)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = catColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            activity.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (activity.isExercise) {
                        Icon(Icons.Default.FitnessCenter,
                            contentDescription = "Exercise",
                            modifier = Modifier.size(14.dp),
                            tint = GreenCompleted)
                    }

                    if (showDate) {
                        Text(
                            activity.date.format(DateTimeFormatter.ofPattern("dd MMM")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Row 3: Duration + time range
                if (!activity.startTime.isNullOrBlank() || !activity.duration.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        val timeText = buildString {
                            if (!activity.startTime.isNullOrBlank()) {
                                append(activity.startTime)
                                if (!activity.endTime.isNullOrBlank()) append(" – ${activity.endTime}")
                            }
                            if (!activity.duration.isNullOrBlank()) {
                                if (isNotEmpty()) append("  ")
                                append("(${activity.duration})")
                            }
                        }
                        Text(
                            timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Row 4: Exercise stats
                if (activity.hasExerciseData()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!activity.distance.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Route, contentDescription = null,
                                    modifier = Modifier.size(12.dp), tint = GreenCompleted)
                                Text("${activity.distance} km",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenCompleted)
                            }
                        }
                        if (activity.calories != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null,
                                    modifier = Modifier.size(12.dp), tint = AmberInProgress)
                                Text("${activity.calories} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberInProgress)
                            }
                        }
                    }
                }

                // Notes preview
                if (!activity.notes.isNullOrBlank()) {
                    Text(
                        activity.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action column: status toggle + delete
            Column(
                modifier = Modifier.padding(end = 8.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status toggle button
                val nextStatus = when (activity.status) {
                    ActivityStatus.Pending    -> ActivityStatus.Completed
                    ActivityStatus.InProgress -> ActivityStatus.Completed
                    ActivityStatus.Completed  -> ActivityStatus.Pending
                    ActivityStatus.Skipped    -> ActivityStatus.Pending
                }
                IconButton(
                    onClick = { onStatusToggle(nextStatus) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        when (activity.status) {
                            ActivityStatus.Completed -> Icons.Default.CheckCircle
                            ActivityStatus.InProgress -> Icons.Default.Pending
                            ActivityStatus.Skipped    -> Icons.Default.Cancel
                            else                      -> Icons.Default.RadioButtonUnchecked
                        },
                        contentDescription = "Toggle status",
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp))
                }
            }
        }
        } // Card
    } // AnimatedVisibility
}

/**
 * Confirmation dialog before deleting an activity.
 */
@Composable
fun DeleteActivityDialog(
    activityName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp))
        },
        title = { Text("Delete Activity?", fontWeight = FontWeight.Bold) },
        text  = {
            Text("\"$activityName\" will be permanently removed from your tracker and Excel file.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Delete") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
