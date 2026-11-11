package com.musicnova.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector


data class PlayerUiState(
    val song: HomeSong? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
}

class PlayerActions(
    val onTogglePlay: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onOpenPlayer: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onToggleLike: () -> Unit = {}
)

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0
)

val DefaultNavItems = listOf(
    NavItem("home", "Home", Icons.Filled.Home),
    NavItem("library", "Library", Icons.Filled.LibraryMusic)
)
