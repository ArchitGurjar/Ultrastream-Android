package com.ultrastream.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.Black,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = TextMain,
    onSurfaceVariant = TextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.Black,
    background = Color(0xFFF3F4F6),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF6B7280)
)

@Composable
fun UltraStreamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
