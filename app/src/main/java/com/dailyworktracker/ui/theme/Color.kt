package com.dailyworktracker.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand Palette ────────────────────────────────────────────────────────────
val Violet80    = Color(0xFFD0BCFF)
val Violet60    = Color(0xFFBB86FC)
val VioletPrimary = Color(0xFF7C3AED)   // Primary brand
val VioletDark  = Color(0xFF5B21B6)

val CyanAccent  = Color(0xFF06B6D4)     // Secondary accent
val CyanLight   = Color(0xFF67E8F9)

// ─── Dark Theme Surfaces ──────────────────────────────────────────────────────
val DarkBackground  = Color(0xFF0D1117)  // Rich dark navy
val DarkSurface     = Color(0xFF161B22)  // Card surfaces
val DarkSurface2    = Color(0xFF1C2333)  // Elevated cards
val DarkSurface3    = Color(0xFF21262D)  // Chips, inputs
val DarkOutline     = Color(0xFF30363D)  // Dividers, borders

// ─── Light Theme Surfaces ─────────────────────────────────────────────────────
val LightBackground = Color(0xFFF8F9FA)
val LightSurface    = Color(0xFFFFFFFF)
val LightSurface2   = Color(0xFFF1F3F5)
val LightOutline    = Color(0xFFE1E4E8)

// ─── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary     = Color(0xFFE6EDF3)   // Dark mode primary text
val TextSecondary   = Color(0xFF8B949E)   // Dark mode secondary text
val TextOnLight     = Color(0xFF1C2128)   // Light mode primary text
val TextSecondOnLight = Color(0xFF57606A) // Light mode secondary text

// ─── Semantic / Status Colors ─────────────────────────────────────────────────
val GreenCompleted  = Color(0xFF22C55E)   // Completed
val GreenLight      = Color(0xFF4ADE80)
val AmberInProgress = Color(0xFFF59E0B)   // In Progress
val AmberLight      = Color(0xFFFBBF24)
val RedPending      = Color(0xFFEF4444)   // Pending / error
val RedLight        = Color(0xFFF87171)
val GraySkipped     = Color(0xFF6B7280)   // Skipped / neutral

// ─── Priority Colors ─────────────────────────────────────────────────────────
val PriorityHigh    = Color(0xFFEF4444)
val PriorityMedium  = Color(0xFFF59E0B)
val PriorityLow     = Color(0xFF22C55E)

// ─── Category Colors (for charts and cards) ───────────────────────────────────
val CategoryStudy         = Color(0xFF3B82F6)  // Blue
val CategoryWork          = Color(0xFF8B5CF6)  // Purple
val CategoryExercise      = Color(0xFF22C55E)  // Green
val CategoryHealth        = Color(0xFFEC4899)  // Pink
val CategoryPersonal      = Color(0xFFF59E0B)  // Amber
val CategoryProject       = Color(0xFF06B6D4)  // Cyan
val CategoryLearning      = Color(0xFF10B981)  // Emerald
val CategoryEntertainment = Color(0xFFF97316)  // Orange
val CategoryOther         = Color(0xFF6B7280)  // Gray

fun categoryColor(category: String): Color = when (category.lowercase()) {
    "study"         -> CategoryStudy
    "work"          -> CategoryWork
    "exercise"      -> CategoryExercise
    "health"        -> CategoryHealth
    "personal"      -> CategoryPersonal
    "project"       -> CategoryProject
    "learning"      -> CategoryLearning
    "entertainment" -> CategoryEntertainment
    else            -> CategoryOther
}

fun statusColor(status: String): Color = when (status.lowercase()) {
    "completed"  -> GreenCompleted
    "inprogress",
    "in progress" -> AmberInProgress
    "pending"    -> RedPending
    "skipped"    -> GraySkipped
    else         -> GraySkipped
}

fun priorityColor(priority: String): Color = when (priority.lowercase()) {
    "high"   -> PriorityHigh
    "medium" -> PriorityMedium
    "low"    -> PriorityLow
    else     -> PriorityMedium
}
