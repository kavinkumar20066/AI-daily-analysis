package com.dailyworktracker.data.model

enum class ActivityPriority(val displayName: String) {
    High("High"),
    Medium("Medium"),
    Low("Low");

    companion object {
        fun fromString(value: String): ActivityPriority = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: Medium
    }
}
