
package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.PlaybackState
import kotlinx.coroutines.delay

private val PlayerBackground = Color(0xFF100D17)
private val PlayerSurface = Color(0xFF211B2B)
private val PlayerPurple = Color(0xFF9277B8)
private val PlayerLavender = Color(0xFFD1C1EB)
private val PlayerMuted = Color(0xFFAAA4B5)

// MINI PLAYER

// MINI PLAYER — LastWave-inspired glass dock

@Composable
fun NovaGlassMiniPlayer(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF332A42),
                        Color(0xFF211B2B),
                        Color(0xFF17141F)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = shape
            )
            .clickable { onExpand() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 9.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = state.artworkUrl,
                contentDescription = "Album artwork",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PlayerSurface)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 13.dp)
            ) {
                Text(
                    text = state.title.ifBlank { "Unknown track" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = state.artist.ifBlank { "Unknown artist" },
                    color = PlayerMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = PlayerPurple,
                contentColor = Color.White,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (state.isPlaying)
                            Icons.Default.Pause
                        else
                            Icons.Default.PlayArrow,
                        contentDescription = "Play or pause",
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .padding(start = 3.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop playback",
                    tint = PlayerMuted,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            PlayerLavender.copy(alpha = 0.42f),
                            PlayerPurple.copy(alpha = 0.58f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}


// FULL PLAYER

@Composable
fun NovaFullPlayerScreen(
    state: PlaybackState,
    musicPlayer: MusicPlayer,
    onClose: () -> Unit,
    onStop: () -> Unit
) {
    var currentPosition by remember {
        mutableFloatStateOf(0f)
    }

    var duration by remember {
        mutableFloatStateOf(1f)
    }

    LaunchedEffect(musicPlayer) {
        while (true) {
            val player = musicPlayer.exoPlayer

            val total = player.duration
            val current = player.currentPosition

            duration = if (total > 0) total.toFloat() else 1f
            currentPosition = current.toFloat()

            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF292037),
                        PlayerBackground,
                        Color(0xFF09080E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // TOP BAR

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 24.dp),
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

                Text(
                    text = "NOW PLAYING",
                    color = PlayerLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.weight(1f))

                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Stop",
                        tint = PlayerMuted
                    )
                }
            }

            Spacer(Modifier.weight(0.7f))

            // ALBUM ARTWORK

            AsyncImage(
                model = state.artworkUrl,
                contentDescription = "Album artwork",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(PlayerSurface)
            )

            Spacer(Modifier.height(35.dp))

            // SONG DETAILS

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.title.ifBlank { "Unknown track" },
                    color = Color.White,
                    fontSize = 23.sp,
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

            Spacer(Modifier.height(25.dp))

            // PROGRESS SLIDER

            Slider(
                value = currentPosition.coerceIn(
                    0f,
                    duration.coerceAtLeast(1f)
                ),
                onValueChange = {
                    currentPosition = it
                },
                onValueChangeFinished = {
                    musicPlayer.seekTo(
                        currentPosition.toLong()
                    )
                },
                valueRange = 0f..duration.coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PlayerLavender,
                    activeTrackColor = PlayerPurple,
                    inactiveTrackColor = PlayerSurface
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlayerTime(currentPosition.toLong()),
                    color = PlayerMuted,
                    fontSize = 11.sp
                )

                Text(
                    text = formatPlayerTime(duration.toLong()),
                    color = PlayerMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            // PLAYBACK CONTROLS

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        musicPlayer.seekTo(0L)
                    }
                ) {
                    Icon(
                        Icons.Default.Replay,
                        contentDescription = "Restart",
                        tint = PlayerMuted,
                        modifier = Modifier.size(27.dp)
                    )
                }

                Spacer(Modifier.width(35.dp))

                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = PlayerPurple,
                    onClick = {
                        musicPlayer.togglePlayPause()
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying)
                                Icons.Default.Pause
                            else
                                Icons.Default.PlayArrow,
                            contentDescription = "Play or pause",
                            tint = Color.White,
                            modifier = Modifier.size(39.dp)
                        )
                    }
                }

                Spacer(Modifier.width(35.dp))

                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = PlayerMuted,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "MUSIC NOVA",
                color = PlayerMuted,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )
        }
    }
}

private fun formatPlayerTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

// FLOATING BOTTOM NAVIGATION

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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(23.dp))
            .background(Color(0xFF1C1824))
            .padding(horizontal = 7.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = selectedTab == item.first

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onSelect(item.first)
                    }
                    .background(
                        if (isSelected)
                            Color(0xFF382C49)
                        else
                            Color.Transparent
                    )
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.second,
                    contentDescription = item.third,
                    tint = if (isSelected)
                        PlayerLavender
                    else
                        PlayerMuted,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.first,
                    color = if (isSelected)
                        PlayerLavender
                    else
                        PlayerMuted,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected)
                        FontWeight.Bold
                    else
                        FontWeight.Normal
                )
            }
        }
    }
}
