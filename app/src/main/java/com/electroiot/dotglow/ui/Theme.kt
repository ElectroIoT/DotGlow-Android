package com.electroiot.dotglow.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Same palette as the clock's web page: near-black panels, LED red accent.
val LedRed = Color(0xFFFF3B3B)
val LedOff = Color(0xFF2B0D0D)
val Panel = Color(0xFF161920)
val PanelLine = Color(0xFF252A35)
val Muted = Color(0xFF8B91A1)
val Good = Color(0xFF22C55E)

private val DotGlowColors = darkColorScheme(
    primary = LedRed,
    onPrimary = Color.White,
    secondary = Color(0xFF93C5FD),
    background = Color(0xFF0C0E13),
    onBackground = Color(0xFFE9EBF1),
    surface = Panel,
    onSurface = Color(0xFFE9EBF1),
    surfaceVariant = Color(0xFF1D212A),
    onSurfaceVariant = Muted,
    outline = PanelLine,
    error = Color(0xFFFF8A8A),
)

/** Dark by design (it mirrors an LED display), whatever the system theme is. */
@Composable
fun DotGlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DotGlowColors, content = content)
}
