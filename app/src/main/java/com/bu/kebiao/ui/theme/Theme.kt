package com.bu.kebiao.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BuPrimary,
    onPrimary = Color.White,
    primaryContainer = BuPrimaryContainer,
    onPrimaryContainer = BuPrimary,
    secondary = BuAccent,
    onSecondary = Color.White,
    secondaryContainer = BuAccentContainer,
    onSecondaryContainer = BuAccent,
    tertiary = BuGreen,
    onTertiary = Color.White,
    tertiaryContainer = BuGreenSoft,
    onTertiaryContainer = BuGreen,
    error = BuRed,
    onError = Color.White,
    errorContainer = BuRedSoft,
    onErrorContainer = BuRed,
    background = BuBackground,
    onBackground = BuInk,
    surface = BuSurface,
    onSurface = BuInk,
    surfaceVariant = BuSurfaceStrong,
    onSurfaceVariant = BuMuted,
    outline = BuLine,
    outlineVariant = BuLine
)

private val DarkColorScheme = darkColorScheme(
    primary = BuPrimaryDark,
    onPrimary = Color(0xFF3D2E14),
    primaryContainer = BuPrimaryContainerDark,
    onPrimaryContainer = BuPrimaryDark,
    secondary = BuAccentDark,
    onSecondary = Color(0xFF1A2540),
    secondaryContainer = BuAccentContainerDark,
    onSecondaryContainer = BuAccentDark,
    tertiary = BuGreenDark,
    onTertiary = Color(0xFF14331F),
    tertiaryContainer = BuGreenSoftDark,
    onTertiaryContainer = BuGreenDark,
    error = BuRedDark,
    onError = Color(0xFF3D1414),
    errorContainer = BuRedSoftDark,
    onErrorContainer = BuRedDark,
    background = BuBackgroundDark,
    onBackground = BuInkDark,
    surface = BuSurfaceDark,
    onSurface = BuInkDark,
    surfaceVariant = BuSurfaceStrongDark,
    onSurfaceVariant = BuMutedDark,
    outline = BuLineDark,
    outlineVariant = BuLineDark
)

@Composable
fun BuKeBiaoTheme(
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
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BuTypography,
        content = content
    )
}
