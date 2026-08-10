package com.dailyworktracker.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.compose.*
import com.dailyworktracker.ui.screens.activities.ActivitiesScreen
import com.dailyworktracker.ui.screens.addedit.AddEditActivityScreen
import com.dailyworktracker.ui.screens.calendar.CalendarScreen
import com.dailyworktracker.ui.screens.daily.DailyActivitiesScreen
import com.dailyworktracker.ui.screens.excel.ExcelManagementScreen
import com.dailyworktracker.ui.screens.home.HomeScreen
import com.dailyworktracker.ui.screens.monthly.MonthlyProgressScreen
import com.dailyworktracker.ui.screens.progress.ProgressScreen
import com.dailyworktracker.ui.screens.search.SearchFilterScreen
import com.dailyworktracker.ui.screens.settings.SettingsScreen
import com.dailyworktracker.ui.screens.summary.DailySummaryScreen
import com.dailyworktracker.ui.screens.weekly.WeeklyScreen
import com.dailyworktracker.ui.screens.yearly.YearlyProgressScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── Routes ───────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object Activities : Screen("activities")
    object Calendar   : Screen("calendar")
    object Progress   : Screen("progress")
    object Settings   : Screen("settings")
    object AddActivity: Screen("add_activity")
    object EditActivity     : Screen("edit_activity/{activityId}") {
        fun createRoute(id: String) = "edit_activity/$id"
    }
    object DailyActivities  : Screen("daily/{dateStr}") {
        fun createRoute(d: String) = "daily/$d"
    }
    object DailySummary     : Screen("summary/{dateStr}") {
        fun createRoute(d: String) = "summary/$d"
    }
    object Weekly           : Screen("weekly/{startDate}") {
        fun createRoute(d: String) = "weekly/$d"
    }
    object MonthlyProgress  : Screen("monthly/{yearMonth}") {
        fun createRoute(ym: String) = "monthly/$ym"
    }
    object YearlyProgress   : Screen("yearly/{year}") {
        fun createRoute(y: String) = "yearly/$y"
    }
    object ExcelManagement  : Screen("excel_management")
    object SearchFilter     : Screen("search")
}

// ─── Bottom Nav Items ─────────────────────────────────────────────────────────
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,       "Home",      Icons.Filled.Home,      Icons.Outlined.Home),
    BottomNavItem(Screen.Activities, "Activities",Icons.Filled.List,      Icons.Outlined.List),
    BottomNavItem(Screen.Calendar,   "Calendar",  Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Progress,   "Progress",  Icons.Filled.BarChart,  Icons.Outlined.BarChart),
    BottomNavItem(Screen.Settings,   "Settings",  Icons.Filled.Settings,  Icons.Outlined.Settings)
)

// ─── App Navigation ───────────────────────────────────────────────────────────
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // Determine if bottom bar should show
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Activities.route,
        Screen.Calendar.route,
        Screen.Progress.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(tonalElevation = 0.dp) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController   = navController,
            startDestination = Screen.Home.route
        ) {
            // ── Bottom Tab Destinations ───────────────────────────────────────

            composable(Screen.Home.route) {
                HomeScreen(
                    contentPadding  = innerPadding,
                    onAddActivity   = { navController.navigate(Screen.AddActivity.route) },
                    onActivityClick = { id ->
                        navController.navigate(Screen.EditActivity.createRoute(id))
                    },
                    onViewAll = { navController.navigate(Screen.Activities.route) }
                )
            }

            composable(Screen.Activities.route) {
                ActivitiesScreen(
                    contentPadding  = innerPadding,
                    onAddActivity   = { navController.navigate(Screen.AddActivity.route) },
                    onActivityClick = { id ->
                        navController.navigate(Screen.EditActivity.createRoute(id))
                    }
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(
                    contentPadding = innerPadding,
                    onDayClick = { dateStr ->
                        navController.navigate(Screen.DailyActivities.createRoute(dateStr))
                    }
                )
            }

            composable(Screen.Progress.route) {
                ProgressScreen(
                    contentPadding = innerPadding,
                    onWeeklyClick  = { d  -> navController.navigate(Screen.Weekly.createRoute(d)) },
                    onMonthlyClick = { ym -> navController.navigate(Screen.MonthlyProgress.createRoute(ym)) },
                    onYearlyClick  = { y  -> navController.navigate(Screen.YearlyProgress.createRoute(y)) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    contentPadding    = innerPadding,
                    onExcelManagement = { navController.navigate(Screen.ExcelManagement.route) }
                )
            }

            // ── Detail Destinations ───────────────────────────────────────────

            composable(Screen.AddActivity.route) {
                AddEditActivityScreen(
                    activityId = null,
                    onSaved = { navController.popBackStack() },
                    onBack  = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditActivity.route,
                arguments = listOf(navArgument("activityId") { type = NavType.StringType })
            ) { backstackEntry ->
                val id = backstackEntry.arguments?.getString("activityId")
                AddEditActivityScreen(
                    activityId = id,
                    onSaved    = { navController.popBackStack() },
                    onBack     = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.DailyActivities.route,
                arguments = listOf(navArgument("dateStr") { type = NavType.StringType })
            ) { backstackEntry ->
                val dateStr = backstackEntry.arguments?.getString("dateStr") ?: LocalDate.now().toString()
                DailyActivitiesScreen(
                    dateString      = dateStr,
                    onAddActivity   = { navController.navigate(Screen.AddActivity.route) },
                    onActivityClick = { id -> navController.navigate(Screen.EditActivity.createRoute(id)) },
                    onBack          = { navController.popBackStack() },
                    onSummaryClick  = { navController.navigate(Screen.DailySummary.createRoute(dateStr)) }
                )
            }

            composable(
                route = Screen.DailySummary.route,
                arguments = listOf(navArgument("dateStr") { type = NavType.StringType })
            ) { entry ->
                DailySummaryScreen(
                    dateString = entry.arguments?.getString("dateStr") ?: LocalDate.now().toString(),
                    onBack     = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Weekly.route,
                arguments = listOf(navArgument("startDate") { type = NavType.StringType })
            ) { entry ->
                WeeklyScreen(
                    startDateString = entry.arguments?.getString("startDate")
                        ?: LocalDate.now().toString(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.MonthlyProgress.route,
                arguments = listOf(navArgument("yearMonth") { type = NavType.StringType })
            ) { entry ->
                MonthlyProgressScreen(
                    yearMonth = entry.arguments?.getString("yearMonth") ?: "",
                    onBack    = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.YearlyProgress.route,
                arguments = listOf(navArgument("year") { type = NavType.StringType })
            ) { entry ->
                YearlyProgressScreen(
                    year   = entry.arguments?.getString("year") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ExcelManagement.route) {
                ExcelManagementScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.SearchFilter.route) {
                SearchFilterScreen(
                    onActivityClick = { id ->
                        navController.navigate(Screen.EditActivity.createRoute(id))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
