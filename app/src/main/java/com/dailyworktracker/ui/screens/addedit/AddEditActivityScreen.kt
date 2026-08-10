package com.dailyworktracker.ui.screens.addedit

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyworktracker.data.model.ActivityPriority
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.Categories
import com.dailyworktracker.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Full Add / Edit Activity form screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditActivityScreen(
    activityId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: AddEditActivityViewModel = viewModel(
        factory = addEditViewModelFactory(application, activityId)
    )

    val uiState      by viewModel.uiState.collectAsState()
    val nameError    by viewModel.nameError.collectAsState()
    val activityName by viewModel.activityName.collectAsState()
    val category     by viewModel.category.collectAsState()
    val customCat    by viewModel.customCategory.collectAsState()
    val date         by viewModel.date.collectAsState()
    val startTime    by viewModel.startTime.collectAsState()
    val endTime      by viewModel.endTime.collectAsState()
    val duration     by viewModel.duration.collectAsState()
    val status       by viewModel.status.collectAsState()
    val priority     by viewModel.priority.collectAsState()
    val notes        by viewModel.notes.collectAsState()
    val isExercise   by viewModel.isExercise.collectAsState()
    val distance     by viewModel.distance.collectAsState()
    val calories     by viewModel.calories.collectAsState()

    // Navigate away on save
    LaunchedEffect(uiState) {
        if (uiState == AddEditUiState.Saved) {
            viewModel.resetSavedState()
            onSaved()
        }
    }

    val scrollState = rememberScrollState()
    val title = if (viewModel.isEditMode) "Edit Activity" else "Add Activity"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState == AddEditUiState.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = { viewModel.save() },
                            enabled = uiState != AddEditUiState.Saving
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error card
            if (uiState is AddEditUiState.Error) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        (uiState as AddEditUiState.Error).message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // ── Section: Basic Info ────────────────────────────────────────────
            SectionHeader("Basic Information")

            // Activity Name
            OutlinedTextField(
                value = activityName,
                onValueChange = viewModel::onActivityNameChange,
                label = { Text("Activity Name *") },
                placeholder = { Text("e.g. Morning Run, Study Session") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )

            // Category Selector
            CategorySelector(
                selected = category,
                onSelect = viewModel::onCategoryChange
            )
            AnimatedVisibility(visible = category == "Custom") {
                OutlinedTextField(
                    value = customCat,
                    onValueChange = viewModel::onCustomCategoryChange,
                    label = { Text("Custom Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Date Picker
            DateField(
                date = date,
                onDateChange = viewModel::onDateChange
            )

            // ── Section: Time ──────────────────────────────────────────────────
            SectionHeader("Time (Optional)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeField(
                    modifier = Modifier.weight(1f),
                    label = "Start Time",
                    value = startTime,
                    onValueChange = viewModel::onStartTimeChange
                )
                TimeField(
                    modifier = Modifier.weight(1f),
                    label = "End Time",
                    value = endTime,
                    onValueChange = viewModel::onEndTimeChange
                )
            }
            OutlinedTextField(
                value = duration,
                onValueChange = viewModel::onDurationChange,
                label = { Text("Duration") },
                placeholder = { Text("e.g. 1h 30m, 90m") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                supportingText = { Text("Auto-computed from start/end time when both filled") }
            )

            // ── Section: Status & Priority ─────────────────────────────────────
            SectionHeader("Status & Priority")
            StatusSelector(selected = status, onSelect = viewModel::onStatusChange)
            PrioritySelector(selected = priority, onSelect = viewModel::onPriorityChange)

            // ── Section: Notes ─────────────────────────────────────────────────
            SectionHeader("Notes (Optional)")
            OutlinedTextField(
                value = notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes") },
                placeholder = { Text("Any additional details…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 6,
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) }
            )

            // ── Section: Exercise ──────────────────────────────────────────────
            SectionHeader("Exercise")
            ExerciseSection(
                isExercise = isExercise,
                distance   = distance,
                calories   = calories,
                onIsExerciseChange = viewModel::onIsExerciseChange,
                onDistanceChange   = viewModel::onDistanceChange,
                onCaloriesChange   = viewModel::onCaloriesChange
            )

            // ── Save Button ────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState != AddEditUiState.Saving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState == AddEditUiState.Saving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Activity", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Bottom padding so FAB doesn't overlap
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Sub-Components ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
}

@Composable
private fun CategorySelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val allOptions = Categories.predefined + "Custom"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Category", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Chip grid
        val chunked = allOptions.chunked(3)
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick  = { onSelect(option) },
                        label    = { Text(option, style = MaterialTheme.typography.labelMedium) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusSelector(
    selected: ActivityStatus,
    onSelect: (ActivityStatus) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Status", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityStatus.entries.forEach { s ->
                val color = when (s) {
                    ActivityStatus.Completed  -> GreenCompleted
                    ActivityStatus.InProgress -> AmberInProgress
                    ActivityStatus.Pending    -> RedPending
                    ActivityStatus.Skipped    -> GraySkipped
                }
                FilterChip(
                    selected = selected == s,
                    onClick  = { onSelect(s) },
                    label    = { Text(s.displayName, style = MaterialTheme.typography.labelMedium) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.25f),
                        selectedLabelColor     = color
                    )
                )
            }
        }
    }
}

@Composable
private fun PrioritySelector(
    selected: ActivityPriority,
    onSelect: (ActivityPriority) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Priority", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityPriority.entries.forEach { p ->
                val color = priorityColor(p.name)
                FilterChip(
                    selected = selected == p,
                    onClick  = { onSelect(p) },
                    label    = { Text(p.displayName, style = MaterialTheme.typography.labelMedium) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.25f),
                        selectedLabelColor     = color
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayFmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")

    OutlinedTextField(
        value = date.format(displayFmt),
        onValueChange = {},
        readOnly = true,
        label = { Text("Date *") },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.EditCalendar, contentDescription = "Pick date")
            }
        }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selected = LocalDate.ofEpochDay(millis / 86_400_000L)
                        onDateChange(selected)
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun TimeField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("HH:mm") },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
    )
}

@Composable
private fun ExerciseSection(
    isExercise: Boolean,
    distance:   String,
    calories:   String,
    onIsExerciseChange: (Boolean) -> Unit,
    onDistanceChange:   (String) -> Unit,
    onCaloriesChange:   (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isExercise)
                GreenCompleted.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (isExercise) GreenCompleted else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Exercise Activity",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = isExercise,
                    onCheckedChange = onIsExerciseChange
                )
            }

            AnimatedVisibility(visible = isExercise) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = distance,
                            onValueChange = onDistanceChange,
                            label = { Text("Distance (km)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) }
                        )
                        OutlinedTextField(
                            value = calories,
                            onValueChange = onCaloriesChange,
                            label = { Text("Calories") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}
