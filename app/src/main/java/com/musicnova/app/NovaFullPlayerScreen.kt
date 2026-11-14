
package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.RepeatMode
import kotlinx.coroutines.delay

@Composable
fun NovaFullPlayerScreen(
    state: com.musicnova.app.playback.PlaybackState,
    musicPlayer: MusicPlayer,
    onClose: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenQueue: () -> Unit,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    val context = LocalContext.current

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

    var palette by remember {
        mutableStateOf<NovaArtworkPalette?>(null)
    }

    LaunchedEffect(state.artworkUrl) {
        palette = state.artworkUrl?.let {
            extractNovaArtworkPalette(context, it)
        }
    }

    LaunchedEffect(state.hasMedia) {
        while (state.hasMedia) {
            duration = musicPlayer.getDuration()
            if (!dragging) {
                position = musicPlayer.getCurrentPosition()
            }
            delay(400)
        }
    }

    val primary = palette?.primary ?: Color(0xFF72558F)
    val secondary = palette?.secondary ?: Color(0xFF392B4B)
    val tertiary = palette?.tertiary ?: Color(0xFF17121F)

    val background = Brush.verticalGradient(
        0f to primary.copy(alpha = 0.58f),
        0.42f to secondary.copy(alpha = 0.38f),
        0.78f to Color(0xFF100D15),
        1f to Color(0xFF08070B)
    )

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF08070B))
    ) {
        val landscape = maxWidth > maxHeight

        Box(
            Modifier
                .fillMaxSize()
                .background(background)
        )

        if (landscape) {
            Row(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(
                    url = state.artworkUrl,
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(0.88f)
                )

                Spacer(Modifier.width(34.dp))

                Column(
                    Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    PlayerHeader(onClose, onStop)

                    Spacer(Modifier.height(18.dp))

                    SongInfo(state.title, state.artist)

                    Spacer(Modifier.height(20.dp))

                    SeekControls(
                        position = position,
                        duration = duration,
                        dragging = dragging,
                        seekValue = seekValue,
                        onDragStart = {
                            dragging = true
                            seekValue = position.toFloat()
                        },
                        onSeekChange = { seekValue = it },
                        onSeekFinish = {
                            musicPlayer.seekTo(seekValue.toLong())
                            position = seekValue.toLong()
                            dragging = false
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    MainControls(
                        isPlaying = state.isPlaying,
                        onPrevious = onPrevious,
                        onToggle = { musicPlayer.togglePlayPause() },
                        onNext = onNext
                    )

                    Spacer(Modifier.height(12.dp))

                    ExtraControls(
                        shuffleEnabled,
                        repeatMode,
                        onToggleShuffle,
                        onCycleRepeat,
                        onOpenQueue
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerHeader(onClose, onStop)

                Spacer(Modifier.weight(0.65f))

                Artwork(
                    url = state.artworkUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(Modifier.weight(0.55f))

                SongInfo(state.title, state.artist)

                Spacer(Modifier.height(22.dp))

                SeekControls(
                    position = position,
                    duration = duration,
                    dragging = dragging,
                    seekValue = seekValue,
                    onDragStart = {
                        dragging = true
                        seekValue = position.toFloat()
                    },
                    onSeekChange = { seekValue = it },
                    onSeekFinish = {
                        musicPlayer.seekTo(seekValue.toLong())
                        position = seekValue.toLong()
                        dragging = false
                    }
                )

                Spacer(Modifier.height(12.dp))

                MainControls(
                    isPlaying = state.isPlaying,
                    onPrevious = onPrevious,
                    onToggle = { musicPlayer.togglePlayPause() },
                    onNext = onNext
                )

                Spacer(Modifier.height(16.dp))

                ExtraControls(
                    shuffleEnabled,
                    repeatMode,
                    onToggleShuffle,
                    onCycleRepeat,
                    onOpenQueue
                )

                Spacer(Modifier.weight(0.65f))
            }
        }
    }
}

@Composable
private fun Artwork(
    url: String?,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = url,
        contentDescription = "Album artwork",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF211B2B))
    )
}

@Composable
private fun PlayerHeader(
    onClose: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Close player",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            "NOW PLAYING",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onStop) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = "Stop playback",
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }
    }
}

@Composable
private fun SongInfo(
    title: String,
    artist: String
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title.ifBlank { "Unknown title" },
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            Text(
                artist.ifBlank { "Unknown artist" },
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SeekControls(
    position: Long,
    duration: Long,
    dragging: Boolean,
    seekValue: Float,
    onDragStart: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinish: () -> Unit
) {
    val max = duration.coerceAtLeast(1L).toFloat()
    val current = if (dragging) seekValue else position.toFloat()

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = current.coerceIn(0f, max),
            onValueChange = {
                if (!dragging) onDragStart()
                onSeekChange(it)
            },
            onValueChangeFinished = onSeekFinish,
            valueRange = 0f..max,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(current.toLong()),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            Text(
                "-${formatTime((duration - current.toLong()).coerceAtLeast(0L))}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MainControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(39.dp)
            )
        }

        Surface(
            onClick = onToggle,
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause
                    else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(39.dp)
                )
            }
        }

        IconButton(onClick = onNext) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(39.dp)
            )
        }
    }
}

@Composable
private fun ExtraControls(
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenQueue: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) Color.White
                else Color.White.copy(alpha = 0.5f)
            )
        }

        IconButton(onClick = onCycleRepeat) {
            Icon(
                if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne
                else Icons.Rounded.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) Color.White
                else Color.White.copy(alpha = 0.5f)
            )
        }

        IconButton(onClick = onOpenQueue) {
            Icon(
                Icons.Rounded.QueueMusic,
                contentDescription = "Queue",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
