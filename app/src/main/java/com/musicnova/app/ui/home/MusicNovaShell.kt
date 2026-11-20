
package com.musicnova.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun MusicNovaShell(
    player: PlayerUiState,
    playerActions: PlayerActions,
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    onReselect: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val hazeState = remember { HazeState() }
    val navItems = remember { DefaultNavItems }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09080E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
        ) {
            content(
                PaddingValues(
                    top = 0.dp,
                    bottom = if (player.song != null) 184.dp else 94.dp
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (player.song != null) {
                ShellMiniPlayer(
                    player = player,
                    actions = playerActions,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 7.dp
                    )
                )
            }

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

@Composable
private fun ShellMiniPlayer(
    player: PlayerUiState,
    actions: PlayerActions,
    modifier: Modifier = Modifier
) {
    val song = player.song ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 14.dp,
        onClick = actions.onOpenPlayer
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xF21A1423),
                            Color(0xE83D2B51),
                            Color(0xE51B1424)
                        )
                    )
                )
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = "Song artwork",
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(15.dp))
            )

            Spacer(Modifier.width(11.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = actions.onTogglePlay) {
                Icon(
                    imageVector = if (player.isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (player.isPlaying) {
                        "Pause"
                    } else {
                        "Play"
                    },
                    tint = Color.White
                )
            }

            IconButton(onClick = actions.onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White
                )
            }

            IconButton(onClick = actions.onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss player",
                    tint = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}
