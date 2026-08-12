package com.dailyworktracker.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * All navigation destinations in the app.
 * Sealed class ensures compile-time exhaustiveness.
 */
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

/** Items shown in the bottom navigation bar. */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
