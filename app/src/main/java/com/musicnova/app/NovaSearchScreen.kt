package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.PlaybackState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val PlayerBackground = Color(0xFF0B0910)
private val PlayerSurface = Color(0xFF211B2B)
private val PlayerPurple = Color(0xFF9277B8)
private val PlayerLavender = Color(0xFFD1C1EB)
private val PlayerMuted = Color(0xFFAAA4B5)

/** LastWave-inspired draggable Now Playing artwork button. */
@Composable
fun NovaGlassMiniPlayer(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    musicPlayer: MusicPlayer? = null
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(musicPlayer, state.title) {
        while (musicPlayer != null) {
            val duration = musicPlayer.getDuration()
            val position = musicPlayer.getCurrentPosition()
            progress = if (duration > 0L) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
            delay(500)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val buttonSize = 68.dp
        val buttonPx = with(density) { buttonSize.toPx() }
        val maxX = (constraints.maxWidth.toFloat() - buttonPx - with(density) { 14.dp.toPx() }).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight.toFloat() - buttonPx - with(density) { 104.dp.toPx() }).coerceAtLeast(0f)
        var offsetX by remember { mutableFloatStateOf(maxX) }
        var offsetY by remember { mutableFloatStateOf(maxY) }
        var dragMoved by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(buttonSize)
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = { dragMoved = false },
                        onDragEnd = { dragMoved = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragMoved = true
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxY)
                        }
                    )
                }
                .clickable {
                    if (!dragMoved) onExpand()
                    dragMoved = false
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(buttonSize)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(PlayerLavender, PlayerPurple, Color(0xFF5A456F))
                        ),
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = Color(0xF21A1522),
                shadowElevation = 14.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        color = PlayerLavender,
                        trackColor = PlayerPurple.copy(alpha = 0.25f),
                        strokeWidth = 3.dp
                    )
                    AsyncImage(
                        model = state.artworkUrl,
                        contentDescription = "Now playing: ${state.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(PlayerSurface)
                    )
                    if (!state.isPlaying) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(5.dp)
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(PlayerPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Paused",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// FULL PLAYER SCREEN
@Composable
fun NovaFullPlayerScreen(
    state: PlaybackState,
    musicPlayer: MusicPlayer,
    onClose: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(musicPlayer) {
        while (true) {
            val total = musicPlayer.getDuration()
            val current = musicPlayer.getCurrentPosition()
            duration = if (total > 0L) total.toFloat() else 1f
            currentPosition = current.coerceAtLeast(0L).toFloat()
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBackground)
    ) {
        AsyncImage(
            model = state.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .align(Alignment.TopCenter)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33000000),
                            Color(0x990B0910),
                            Color(0xFF0B0910),
                            Color(0xFF0B0910)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "NOW PLAYING",
                        color = PlayerLavender,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "MUSIC NOVA",
                        color = PlayerMuted,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Stop playback",
                        tint = PlayerMuted
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = state.title.ifBlank { "Unknown track" },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = state.artist.ifBlank { "Unknown artist" },
                    color = PlayerMuted,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(20.dp))
            Slider(
                value = currentPosition.coerceIn(0f, duration.coerceAtLeast(1f)),
                onValueChange = { currentPosition = it },
                onValueChangeFinished = {
                    musicPlayer.seekTo(currentPosition.toLong())
                },
                valueRange = 0f..duration.coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PlayerLavender,
                    activeTrackColor = PlayerPurple,
                    inactiveTrackColor = Color(0xFF51485D)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatPlayerTime(currentPosition.toLong()), color = PlayerMuted, fontSize = 11.sp)
                Text(formatPlayerTime(duration.toLong()), color = PlayerMuted, fontSize = 11.sp)
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = PlayerLavender,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.width(25.dp))
                Surface(
                    onClick = { musicPlayer.togglePlayPause() },
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = PlayerPurple,
                    contentColor = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or pause",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(Modifier.width(25.dp))
                IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next track",
                        tint = PlayerLavender,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(Modifier.weight(0.7f))
            Text(
                "FEEL THE SOUND",
                color = PlayerLavender.copy(alpha = 0.7f),
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 17.dp)
            )
        }
    }
}

private fun formatPlayerTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

// FLOATING BOTTOM NAVIGATION - shown only when no track is playing.
@Composable
fun NovaBottomNavigation(
    selectedTab: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("Home", Icons.Default.Home, "Home"),
        Triple("Search", Icons.Default.Search, "Search"),
        Triple("Library", Icons.Default.LibraryMusic, "Library"),
        Triple("Profile", Icons.Default.Person, "Profile")
    )
    val shape = RoundedCornerShape(26.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF292333), Color(0xFF1C1825), Color(0xFF15121C))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = selectedTab == item.first
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(item.first) }
                    .background(
                        if (isSelected) Color(0xFF40334F).copy(alpha = 0.85f)
                        else Color.Transparent
                    )
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.second,
                    contentDescription = item.third,
                    tint = if (isSelected) PlayerLavender else PlayerMuted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.first,
                    color = if (isSelected) PlayerLavender else PlayerMuted,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
