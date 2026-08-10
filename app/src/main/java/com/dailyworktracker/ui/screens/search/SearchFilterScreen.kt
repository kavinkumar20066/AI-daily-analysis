package com.dailyworktracker.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailyworktracker.data.model.ActivityStatus
import com.dailyworktracker.data.model.Categories
import com.dailyworktracker.ui.components.ActivityCard
import com.dailyworktracker.ui.components.DeleteActivityDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterScreen(
    onActivityClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val viewModel: SearchFilterViewModel = remember {
        SearchFilterViewModel(application)
    }

    val filter       by viewModel.filter.collectAsState()
    val results      by viewModel.results.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val pendingDelete by viewModel.pendingDelete.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = filter.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search activities…") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (filter.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        }
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Filter Chips ──────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status filters
                items(ActivityStatus.entries.toList()) { status ->
                    FilterChip(
                        selected = filter.status == status.name,
                        onClick  = {
                            viewModel.onStatusChange(
                                if (filter.status == status.name) "" else status.name
                            )
                        },
                        label = { Text(status.displayName) }
                    )
                }

                // Exercise filter
                item {
                    FilterChip(
                        selected = filter.isExerciseOnly,
                        onClick  = { viewModel.onExerciseOnlyChange(!filter.isExerciseOnly) },
                        label    = { Text("Exercise") },
                        leadingIcon = { Icon(Icons.Default.FitnessCenter, null,
                            modifier = Modifier.size(16.dp)) }
                    )
                }

                // Category filters
                items(allCategories) { cat ->
                    FilterChip(
                        selected = filter.category == cat,
                        onClick  = {
                            viewModel.onCategoryChange(if (filter.category == cat) "" else cat)
                        },
                        label = { Text(cat) }
                    )
                }
            }

            // Clear filters button
            val hasFilters = filter.query.isNotEmpty() || filter.status.isNotEmpty()
                || filter.category.isNotEmpty() || filter.isExerciseOnly
            if (hasFilters) {
                TextButton(
                    onClick = { viewModel.clearFilters() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.FilterListOff, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear all filters")
                }
            }

            HorizontalDivider()

            // ── Results count ─────────────────────────────────────────────────
            Text(
                "${results.size} ${if (results.size == 1) "result" else "results"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Results list ──────────────────────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = results, key = { it.id }) { activity ->
                    ActivityCard(
                        activity       = activity,
                        showDate       = true,
                        onClick        = { onActivityClick(activity.id) },
                        onStatusToggle = { ns -> viewModel.toggleStatus(activity, ns) },
                        onDelete       = { viewModel.requestDelete(activity) }
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
