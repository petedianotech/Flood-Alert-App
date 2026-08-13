package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = GoogleBlueDark,
    onPrimaryContainer = GoogleBlueLight,
    secondary = SlateGrey,
    onSecondary = Color.White,
    secondaryContainer = NeutralContainerDark,
    onSecondaryContainer = NeutralContainerLight,
    surface = SurfaceDark,
    onSurface = Color(0xFFE3E2E6),
    background = BackgroundDark,
    onBackground = Color(0xFFE3E2E6),
    error = MaterialRedDark,
    onError = Color.Black,
    errorContainer = MaterialRed,
    onErrorContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = GoogleBlueLight,
    onPrimaryContainer = GoogleBlueDark,
    secondary = SlateGrey,
    onSecondary = Color.White,
    secondaryContainer = NeutralContainerLight,
    onSecondaryContainer = SlateGrey,
    surface = SurfaceLight,
    onSurface = Color(0xFF1F1F1F),
    background = BackgroundLight,
    onBackground = Color(0xFF1F1F1F),
    error = MaterialRed,
    onError = Color.White,
    errorContainer = MaterialRedContainer,
    onErrorContainer = MaterialRed
)

@Composable
fun FloodAlertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to preserve strict Google Workspace branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = FloodAlertTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

