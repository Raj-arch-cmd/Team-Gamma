package com.example.team_gamma.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Updated Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = LightPurpleBackground,
    background = SurfaceLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = DarkText,
    onBackground = DarkText,
    onSurface = DarkText,
    error = AccentRed
)

// A corresponding Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = LightPurpleBackground,
    background = SurfaceDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = LightText,
    onBackground = LightText,
    onSurface = LightText,
    error = AccentRed
)

@Composable
fun TeamGammaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
