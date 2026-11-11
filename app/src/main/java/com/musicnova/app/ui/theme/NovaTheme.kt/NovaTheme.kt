package com.musicnova.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object NovaColors {
    val Background = Color(0xFF09080E)
    val Surface = Color(0xFF191620)
    val Card = Color(0xFF211C2B)

    val Purple = Color(0xFF9277B8)
    val Lavender = Color(0xFFC9B9E5)

    val Text = Color(0xFFF7F4FC)
    val Muted = Color(0xFFAAA4B5)

    val GlassBorder = Color.White.copy(alpha = 0.09f)
    val GlassSurface = Color(0xFF211C2B).copy(alpha = 0.78f)
}

val NovaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = NovaColors.Purple,
        secondary = NovaColors.Lavender,
        background = NovaColors.Background,
        surface = NovaColors.Surface,
        surfaceVariant = NovaColors.Card,
        onPrimary = NovaColors.Text,
        onSecondary = NovaColors.Background,
        onBackground = NovaColors.Text,
        onSurface = NovaColors.Text,
        onSurfaceVariant = NovaColors.Muted,
        outline = NovaColors.GlassBorder
    )

    MaterialTheme(
    colorScheme = colors,
    typography = LastWaveTypography,
    shapes = NovaShapes,
    content = content
)
}
