
package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.playback.PlaybackQueueState

private val QueueBg = Color(0xFF09080E)
private val QueueCard = Color(0xFF191620)
private val QueuePurple = Color(0xFF9277B8)
private val QueueMuted = Color(0xFFAAA4B5)

@Composable
fun QueueScreen(
    state: PlaybackQueueState,
    onBack: () -> Unit,
    onPlay: (YouTubeMusicTrack) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QueueBg)
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    "Playing Queue",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${state.tracks.size} songs",
                    color = QueueMuted,
                    fontSize = 12.sp
                )
            }

            if (state.tracks.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Clear queue",
                        tint = Color(0xFFFF8585)
                    )
                }
            }
        }

        if (state.tracks.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = QueuePurple,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Your queue is empty",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Add songs to start building your queue",
                        color = QueueMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            val current = state.currentTrack

            if (current != null) {
                Text(
                    "NOW PLAYING",
                    color = QueuePurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(QueueCard)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = current.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            current.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            current.artist,
                            color = QueueMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = QueuePurple
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "UP NEXT",
                    color = QueuePurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${(state.tracks.size - state.currentIndex - 1).coerceAtLeast(0)} songs",
                    color = QueueMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                itemsIndexed(
                    state.tracks,
                    key = { _, track -> track.videoId }
                ) { index, track ->
                    val isCurrent = index == state.currentIndex

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (isCurrent) QueuePurple.copy(alpha = 0.16f)
                                else QueueCard
                            )
                            .clickable { onPlay(track) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                track.title,
                                color = if (isCurrent) QueuePurple else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                track.artist,
                                color = QueueMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    if (index > 0) onMove(index, index - 1)
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    tint = if (index > 0) Color.White else QueueMuted
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (index < state.tracks.lastIndex) {
                                        onMove(index, index + 1)
                                    }
                                },
                                enabled = index < state.tracks.lastIndex,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = if (index < state.tracks.lastIndex) Color.White else QueueMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove song",
                                tint = Color(0xFFFF8585)
                            )
                        }
                    }
                }
            }
        }
    }
}
