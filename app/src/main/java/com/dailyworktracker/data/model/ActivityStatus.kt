package com.dailyworktracker.data.model

enum class ActivityStatus(val displayName: String) {
    Completed("Completed"),
    InProgress("In Progress"),
    Pending("Pending"),
    Skipped("Skipped");

    companion object {
        fun fromString(value: String): ActivityStatus = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true) ||
            it.displayName.equals(value, ignoreCase = true)
        } ?: Pending
    }
}
