package com.example.ui.theme

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
    primary = JarvisCyan,
    onPrimary = JarvisNavyDark,
    primaryContainer = JarvisCardDark,
    onPrimaryContainer = JarvisCyanLight,
    secondary = JarvisGold,
    onSecondary = JarvisNavyDark,
    secondaryContainer = JarvisCardDark,
    onSecondaryContainer = JarvisGold,
    background = JarvisNavyDark,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurfaceDark,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisCardDark,
    onSurfaceVariant = JarvisTextSecondary,
    error = JarvisRedAlert,
    onError = JarvisTextPrimary
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.statusBarColor = JarvisNavyDark.toArgb()
            window?.navigationBarColor = JarvisNavyDark.toArgb()
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
