package com.onelabs.oneplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OneColorScheme = darkColorScheme(
    primary = OneRed,
    onPrimary = TextWhite,
    primaryContainer = OneSurfaceVariant,
    onPrimaryContainer = TextWhite,
    secondary = OneCrimson,
    onSecondary = TextWhite,
    tertiary = OneGreen,
    background = OneBlack,
    onBackground = TextWhite,
    surface = OneDarkSurface,
    onSurface = TextWhite,
    surfaceVariant = OneCardSurface,
    onSurfaceVariant = TextGray,
    outline = OneBorder,
    error = OneRed
)

@Composable
fun ONEPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OneColorScheme,
        content = content
    )
}
