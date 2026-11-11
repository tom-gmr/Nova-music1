package com.musicnova.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun MusicNovaShell(
    player: PlayerUiState,
    playerActions: PlayerActions,
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    navItems: List<NavItem> = DefaultNavItems,
    onReselect: (String) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    var barsHeightPx by remember { mutableStateOf(0) }
    val barsHeight = with(density) { barsHeightPx.toDp() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier.fillMaxSize().background(HomeColors.Bg)) {
        Box(Modifier.fillMaxSize().hazeSource(state = hazeState)) {
            content(PaddingValues(top = statusBarTop, bottom = barsHeight + 16.dp))
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { barsHeightPx = it.height }
        ) {
            GlassMiniPlayer(
                state = player,
                actions = playerActions,
                hazeState = hazeState
            )
            GlassBottomNav(
                items = navItems,
                selectedRoute = selectedRoute,
                onSelect = onNavigate,
                onReselect = onReselect,
                hazeState = hazeState
            )
        }
    }
}
