package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = SpaceNavyDark,
    primaryContainer = SpaceNavyLight,
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = Color.White,
    secondaryContainer = SpaceNavyLight,
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonGold,
    onTertiary = SpaceNavyDark,
    background = SpaceNavyDark,
    onBackground = TextWhite,
    surface = SpaceNavyCard,
    onSurface = TextWhite,
    surfaceVariant = SpaceNavyBorder,
    onSurfaceVariant = TextMuted,
    outline = SpaceNavyBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Bricks Breaker Quest is best experienced in dark arcade theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
