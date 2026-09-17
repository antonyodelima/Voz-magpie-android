package com.example.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VozoPrimary,
    onPrimary = VozoDarkBg,
    primaryContainer = VozoDarkSurfaceVariant,
    onPrimaryContainer = VozoAccent,
    secondary = VozoSecondary,
    onSecondary = VozoTextPrimary,
    background = VozoDarkBg,
    onBackground = VozoTextPrimary,
    surface = VozoDarkSurface,
    onSurface = VozoTextPrimary,
    surfaceVariant = VozoDarkSurfaceVariant,
    onSurfaceVariant = VozoTextSecondary,
    error = VozoError,
    onError = VozoTextPrimary,
    outline = VozoBorder
)

@Composable
fun VozoMagpieTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VozoDarkBg.toArgb()
                window.navigationBarColor = VozoDarkBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
