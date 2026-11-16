package com.example.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.core.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = ContextaIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = ContextaIndigoContainer,
    onPrimaryContainer = ContextaOnIndigoContainer,
    secondary = ContextaEmeraldSuccess,
    onSecondary = Color.White,
    secondaryContainer = ContextaEmeraldContainer,
    onSecondaryContainer = ContextaOnEmeraldContainer,
    tertiary = ContextaAmberWarning,
    tertiaryContainer = ContextaAmberContainer,
    background = ContextaLightBackground,
    onBackground = ContextaLightOnBackground,
    surface = ContextaLightSurface,
    onSurface = ContextaLightOnBackground,
    surfaceVariant = ContextaLightSurfaceVariant,
    onSurfaceVariant = ContextaLightOnSurfaceVariant,
    surfaceContainerLow = ContextaLightSurfaceContainer,
    outline = ContextaLightOutline,
    outlineVariant = ContextaLightOutline,
    error = ContextaError,
    errorContainer = ContextaErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = ContextaDarkIndigoPrimary,
    onPrimary = Color(0xFF0E1523),
    primaryContainer = ContextaDarkIndigoContainer,
    onPrimaryContainer = ContextaDarkOnIndigoContainer,
    secondary = ContextaDarkEmeraldSuccess,
    onSecondary = Color(0xFF0B2520),
    secondaryContainer = ContextaDarkEmeraldContainer,
    onSecondaryContainer = ContextaDarkOnEmeraldContainer,
    tertiary = Color(0xFFFFCA7A),
    tertiaryContainer = Color(0xFF4B3207),
    background = ContextaDarkBackground,
    onBackground = ContextaDarkOnBackground,
    surface = ContextaDarkSurface,
    onSurface = ContextaDarkOnBackground,
    surfaceVariant = ContextaDarkSurfaceVariant,
    onSurfaceVariant = ContextaDarkOnSurfaceVariant,
    surfaceContainerLow = ContextaDarkSurfaceContainer,
    outline = ContextaDarkOutline,
    outlineVariant = ContextaDarkOutline,
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF601410)
)

@Composable
fun ContextaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ContextaTypography,
        shapes = ContextaShapes,
        content = content
    )
}
