package com.neo.flashcard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color.White,
    primaryContainer = DarkBg2,
    onPrimaryContainer = DarkText,
    secondary = EasyTeal,
    onSecondary = Color.Black,
    error = HardRed,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkBg2,
    onSurface = DarkText,
    surfaceVariant = DarkBg3,
    onSurfaceVariant = DarkTextMuted,
    outline = Color(0xFF2D3D4D)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF5F7FA),
    onSurface = Color.Black
)

@Composable
fun NeoFlashcardTheme(
    darkTheme: Boolean = true, // Force dark theme as per production spec
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
