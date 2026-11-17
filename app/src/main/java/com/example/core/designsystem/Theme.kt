package com.example.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.core.model.ThemeMode

@Immutable
data class ContextaExtendedColors(
    val navySurface: Color,
    val navySurfaceEnd: Color,
    val heroAccentLine: Color,
    val onNavySurface: Color,
    val onNavyMuted: Color,
    val navyBorder: Color,
    val navyPillBg: Color,
    val navyTrackBg: Color,
    val navyProgressFill: Color,
    val navyActionBg: Color,
    val navyActionText: Color,
    val barTop: Color,
    val barBottom: Color,
    val navIndicator: Color,
    val cardBorder: Color,
    val cardShadow: Color
)

private val LightExtendedColors = ContextaExtendedColors(
    navySurface = ContextaNavySurface,
    navySurfaceEnd = ContextaNavySurfaceEnd,
    heroAccentLine = ContextaHeroAccentLine,
    onNavySurface = Color.White,
    onNavyMuted = Color(0xFFCBD5E1),
    navyBorder = ContextaNavySurfaceBorder,
    navyPillBg = Color(0xFF1D2E52),
    navyTrackBg = Color(0xFF1E2F54),
    navyProgressFill = Color(0xFF60A5FA),
    navyActionBg = Color.White,
    navyActionText = Color(0xFF0F172A),
    barTop = ContextaBarGradientTop,
    barBottom = ContextaBarGradientBottom,
    navIndicator = ContextaNavIndicatorLight,
    cardBorder = ContextaLightCardBorder,
    cardShadow = ContextaLightCardShadow
)

private val DarkExtendedColors = ContextaExtendedColors(
    navySurface = ContextaDarkNavySurface,
    navySurfaceEnd = Color(0xFF162442),
    heroAccentLine = Color(0xFF334E85),
    onNavySurface = ContextaDarkOnBackground,
    onNavyMuted = ContextaDarkOnSurfaceVariant,
    navyBorder = ContextaDarkNavySurfaceBorder,
    navyPillBg = Color(0xFF334155),
    navyTrackBg = Color(0xFF334155),
    navyProgressFill = ContextaDarkBluePrimary,
    navyActionBg = Color.White,
    navyActionText = Color(0xFF0F172A),
    barTop = Color(0xFF60A5FA),
    barBottom = ContextaDarkBluePrimary,
    navIndicator = ContextaDarkBlueContainer,
    cardBorder = ContextaDarkCardBorder,
    cardShadow = ContextaDarkCardShadow
)

val LocalContextaColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = ContextaBluePrimary,
    onPrimary = Color.White,
    primaryContainer = ContextaBlueContainer,
    onPrimaryContainer = ContextaOnBlueContainer,
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
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = ContextaLightBackground,
    surfaceContainerHigh = ContextaLightSurfaceVariant,
    outline = ContextaLightOutline,
    outlineVariant = ContextaLightCardBorder,
    error = ContextaError,
    errorContainer = ContextaErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = ContextaDarkBluePrimary,
    onPrimary = Color.White,
    primaryContainer = ContextaDarkBlueContainer,
    onPrimaryContainer = ContextaDarkOnBlueContainer,
    secondary = ContextaDarkEmeraldSuccess,
    onSecondary = Color(0xFF052E16),
    secondaryContainer = ContextaDarkEmeraldContainer,
    onSecondaryContainer = ContextaDarkOnEmeraldContainer,
    tertiary = Color(0xFFFBBF24),
    tertiaryContainer = Color(0xFF451A03),
    background = ContextaDarkBackground,
    onBackground = ContextaDarkOnBackground,
    surface = ContextaDarkSurface,
    onSurface = ContextaDarkOnBackground,
    surfaceVariant = ContextaDarkSurfaceVariant,
    onSurfaceVariant = ContextaDarkOnSurfaceVariant,
    surfaceContainerLowest = ContextaDarkBackground,
    surfaceContainerLow = ContextaDarkSurface,
    surfaceContainer = ContextaDarkSurfaceContainer,
    surfaceContainerHigh = ContextaDarkSurfaceVariant,
    outline = ContextaDarkOutline,
    outlineVariant = ContextaDarkCardBorder,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D)
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
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalContextaColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ContextaTypography,
            shapes = ContextaShapes,
            content = content
        )
    }
}
