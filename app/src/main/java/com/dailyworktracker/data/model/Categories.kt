package com.dailyworktracker.data.model

object Categories {
    val predefined = listOf(
        "Study",
        "Work",
        "Exercise",
        "Health",
        "Personal",
        "Project",
        "Learning",
        "Entertainment",
        "Other"
    )

    val exerciseTypes = listOf(
        "Walking",
        "Running",
        "Gym",
        "Cycling",
        "Yoga",
        "Workout",
        "Sports",
        "Other"
    )

    // Category icon mapping (Material Icons names for reference)
    fun iconForCategory(category: String): String = when (category.lowercase()) {
        "study" -> "MenuBook"
        "work" -> "Work"
        "exercise" -> "FitnessCenter"
        "health" -> "HealthAndSafety"
        "personal" -> "Person"
        "project" -> "Assignment"
        "learning" -> "School"
        "entertainment" -> "SportsEsports"
        else -> "Category"
    }
}
