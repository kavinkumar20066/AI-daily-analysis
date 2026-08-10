package com.dailyworktracker.ui.navigation

/**
 * All navigation destinations in the app.
 * Sealed class ensures compile-time exhaustiveness.
 */
sealed class Screen(val route: String) {

    // ─── Top-level (Bottom Nav) ───────────────────────────────────────────────
    object Home       : Screen("home")
    object Activities : Screen("activities")
    object History    : Screen("history")
    object Progress   : Screen("progress")
    object Settings   : Screen("settings")

    // ─── Detail / Secondary ───────────────────────────────────────────────────
    object AddActivity : Screen("add_activity")

    object EditActivity : Screen("edit_activity/{activityId}") {
        fun createRoute(activityId: String) = "edit_activity/$activityId"
    }

    object DailyActivities : Screen("daily_activities/{date}") {
        fun createRoute(date: String) = "daily_activities/$date"
    }

    object DailySummary : Screen("daily_summary/{date}") {
        fun createRoute(date: String) = "daily_summary/$date"
    }

    object WeeklyView : Screen("weekly_view/{startDate}") {
        fun createRoute(startDate: String) = "weekly_view/$startDate"
    }

    object MonthlyProgress : Screen("monthly_progress/{yearMonth}") {
        fun createRoute(yearMonth: String) = "monthly_progress/$yearMonth"
    }

    object YearlyProgress : Screen("yearly_progress/{year}") {
        fun createRoute(year: String) = "yearly_progress/$year"
    }

    object ExcelManagement : Screen("excel_management")
    object SearchFilter    : Screen("search_filter")
}

/** Items shown in the bottom navigation bar. */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val iconUnselected: Int, // drawable resource or use vector icon in composable
    val iconSelected: Int
)
