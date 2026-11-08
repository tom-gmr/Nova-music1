package com.musicnova.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NovaColors {
    val Background = Color(0xFF09080D)
    val Surface = Color(0xFF17141E)
    val Card = Color(0xFF211C2B)

    val Purple = Color(0xFF9277B8)
    val Lavender = Color(0xFFC9B9E5)

    val Text = Color(0xFFF7F4FC)
    val Muted = Color(0xFFAAA4B5)
}
@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = NovaColors.Purple,
        secondary = NovaColors.Lavender,
        background = NovaColors.Background,
        surface = NovaColors.Surface,
        onPrimary = NovaColors.Text,
        onSecondary = NovaColors.Background,
        onBackground = NovaColors.Text,
        onSurface = NovaColors.Text
    )

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
