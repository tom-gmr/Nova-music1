package com.musicnova.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musicnova.app.playback.AudioSignalPathScreen
import com.musicnova.app.playback.AudioSignalPathStore
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.PlaybackState
import kotlinx.coroutines.delay

@Composable
fun NovaFullPlayerScreen(
    state: PlaybackState,
    musicPlayer: MusicPlayer,
    onClose: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var showSignalPath by remember { mutableStateOf(false) }

    val signalPath by AudioSignalPathStore.state.collectAsState()

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = showSignalPath) {
        showSignalPath = false
    }

    LaunchedEffect(state.hasMedia) {
        while (state.hasMedia) {
            positionMs = musicPlayer.getCurrentPosition()
            durationMs = musicPlayer.getDuration()
            delay(500)
        }

        positionMs = 0L
        durationMs = 0L
    }

    if (showSignalPath) {
        AudioSignalPathScreen(
            onBack = { showSignalPath = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09080E))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) {
                    Text("Close")
                }

                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFAAA4B5),
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onStop) {
                    Text("Stop", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!state.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = "Album artwork",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF191620)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♪",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color(0xFFC9B9E5)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = state.title.ifBlank { "Unknown title" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = state.artist.ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFAAA4B5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp)
            )

            Spacer(Modifier.height(22.dp))

            // Tap this row to open Signal Path diagnostics.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF191620))
                    .clickable { showSignalPath = true }
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Audio quality",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFAAA4B5)
                    )

                    Text(
                        text = buildString {
                            append(signalFormatLabel(signalPath.source.mimeType))
                            append(" • ")
                            append(
                                signalPath.source.bitrate?.let {
                                    "${(it + 500) / 1000} kbps"
                                } ?: "Bitrate unavailable"
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFC9B9E5)
                )
            }

            Spacer(Modifier.height(18.dp))

            val safeDuration = durationMs.coerceAtLeast(1L)
            Slider(
                value = positionMs.coerceIn(0L, safeDuration).toFloat(),
                onValueChange = { musicPlayer.seekTo(it.toLong()) },
                valueRange = 0f..safeDuration.toFloat(),
                enabled = durationMs > 0L,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlayerTime(positionMs),
                    color = Color(0xFFAAA4B5),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatPlayerTime(durationMs),
                    color = Color(0xFFAAA4B5),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onPrevious) {
                    Text("Previous")
                }

                Button(
                    onClick = { musicPlayer.togglePlayPause() },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(if (state.isPlaying) "Pause" else "Play")
                }

                FilledTonalButton(onClick = onNext) {
                    Text("Next")
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

private fun signalFormatLabel(mimeType: String?): String {
    return when (mimeType?.lowercase()) {
        "audio/opus" -> "Opus"
        "audio/flac" -> "FLAC"
        "audio/mpeg" -> "MP3"
        "audio/aac", "audio/mp4a-latm" -> "AAC"
        "audio/vorbis" -> "Vorbis"
        "audio/alac" -> "ALAC"
        "audio/wav", "audio/raw" -> "PCM"
        null, "" -> "Audio format unavailable"
        else -> mimeType.substringAfter("/").uppercase()
    }
}

private fun formatPlayerTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
