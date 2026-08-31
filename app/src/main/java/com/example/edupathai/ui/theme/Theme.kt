package com.example.edupathai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = TextPrimaryWhite,
    primaryContainer = NavySurfaceVariant,
    onPrimaryContainer = SkyCyan,
    secondary = SkyCyan,
    onSecondary = MidnightDark,
    background = MidnightDark,
    onBackground = TextPrimaryWhite,
    surface = NavySurface,
    onSurface = TextPrimaryWhite,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextMutedSlate,
    outline = NavyBorder,
    outlineVariant = NavyBorderLight,
    error = CoralError,
    onError = TextPrimaryWhite
)

@Composable
fun EduPathAITheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MidnightDark.toArgb()
            window.navigationBarColor = MidnightDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}