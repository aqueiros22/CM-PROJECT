package com.example.fieldsense.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat



// Map our custom colors to the Material 3 color system
private val LightColorScheme = lightColorScheme(
    primary = FieldSenseGreen,         // Main brand color
    onPrimary = TextPrimary,           // Black text on top of primary (green) buttons
    secondary = FieldSenseNavy,        // Secondary brand color (Dark Blue)
    onSecondary = Color.White,         // White text on top of secondary buttons
    background = BackgroundLight,      // App background color
    onBackground = TextPrimary,        // Text color on the background
    surface = SurfaceLight,            // Card background color
    onSurface = TextPrimary            // Text color on cards
)

@Composable
fun FieldSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}