package com.example.problemreportapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ErrorColor = Color(0xFFE65C4F) // Red for error messages
private val ExtraColor = Color(0xFFaaff0d) // Red for error messages
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF36392D),
    secondary = Color(0xFFF7F7F7),
    tertiary = Color(0xFFB5D43B),
    error = ErrorColor,
    background = Color(0xFF2A3135)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF36392D),
    secondary = Color(0xFFF7F7F7),
    tertiary = Color(0xFFB5D43B),
    error = ErrorColor,
    background = Color(0xFFF7F7F7)
)

@Composable
fun ProblemReportAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
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