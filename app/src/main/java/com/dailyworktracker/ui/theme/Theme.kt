package com.dailyworktracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Dark Color Scheme ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = VioletPrimary,
    onPrimary          = TextPrimary,
    primaryContainer   = VioletDark,
    onPrimaryContainer = Violet80,
    secondary          = CyanAccent,
    onSecondary        = DarkBackground,
    secondaryContainer = Color(0xFF004A57),
    onSecondaryContainer = CyanLight,
    tertiary           = GreenCompleted,
    onTertiary         = DarkBackground,
    background         = DarkBackground,
    onBackground       = TextPrimary,
    surface            = DarkSurface,
    onSurface          = TextPrimary,
    surfaceVariant     = DarkSurface2,
    onSurfaceVariant   = TextSecondary,
    outline            = DarkOutline,
    error              = RedPending,
    onError            = TextPrimary
)

// ─── Light Color Scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = VioletPrimary,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFEDE9FE),
    onPrimaryContainer = VioletDark,
    secondary          = Color(0xFF0284C7),
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary           = Color(0xFF16A34A),
    onTertiary         = Color.White,
    background         = LightBackground,
    onBackground       = TextOnLight,
    surface            = LightSurface,
    onSurface          = TextOnLight,
    surfaceVariant     = LightSurface2,
    onSurfaceVariant   = TextSecondOnLight,
    outline            = LightOutline,
    error              = RedPending,
    onError            = Color.White
)

/**
 * App theme with Light / Dark / System support.
 * Uses our brand violet/cyan palette — dynamic color disabled for brand consistency.
 */
@Composable
fun DailyWorkTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
