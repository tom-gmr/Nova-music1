package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
private val PlayerBackground = Color(0xFF100D17)
private val PlayerSurface = Color(0xFF211B2B)
private val PlayerPurple = Color(0xFF9277B8)
private val PlayerLavender = Color(0xFFD1C1EB)
private val PlayerMuted = Color(0xFFAAA4B5)

// MINI PLAYER

@Composable
fun NovaGlassMiniPlayer(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(100.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(shape)
            .background(Color(0xE6101515))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.48f),
                shape
            )
            .clickable { onExpand() }
    ) {
        // Soft glass reflections
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x443B7774),
                            Color(0x22202020),
                            Color(0x77B52D78),
                            Color(0x668E5C91)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 12.dp,
                    end = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = state.artworkUrl,
                contentDescription = "Album artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PlayerSurface)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.title.ifBlank {
                        "Unknown track"
                    },
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = state.artist.ifBlank {
                        "Unknown artist"
                    },
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = Color(0xFFEBC9C9),
                contentColor = Color(0xFF321B20),
                modifier = Modifier.size(66.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (state.isPlaying)
                            Icons.Default.Pause
                        else
                            Icons.Default.PlayArrow,
                        contentDescription = "Play or pause",
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Surface(
                onClick = onNext,
                shape = CircleShape,
                color = Color(0xFF38282D),
                contentColor = Color(0xFFE8CACA),
                modifier = Modifier.size(66.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next track",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop playback",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}


// FULL PLAYER

@Composable
fun NovaFullPlayerScreen(
    state: PlaybackState,
    musicPlayer: MusicPlayer,
    onClose: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    var currentPosition by remember {
        mutableFloatStateOf(0f)
    }

    var duration by remember {
        mutableFloatStateOf(1f)
    }

    LaunchedEffect(musicPlayer) {
        while (true) {
            val total = musicPlayer.getDuration()
            val current = musicPlayer.getCurrentPosition()

            duration = if (total > 0L) {
                total.toFloat()
            } else {
                1f
            }

            currentPosition =
                current.coerceAtLeast(0L).toFloat()

            delay(500)
        }
    }

    val playerShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF30243F),
                        Color(0xFF17131F),
                        Color(0xFF09080E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp),
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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

            Spacer(Modifier.weight(0.5f))

            Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
) {
    // Current song artwork as full-screen background
    AsyncImage(
        model = state.artworkUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )

    // Dark fade towards the bottom
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x22000000),
                        Color(0x44000000),
                        Color(0xCC090609),
                        Color(0xFF08070B)
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
        // Yahan tumhara existing Column content bilkul same rahega.
            ) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescri

            Spacer(Modifier.height(30.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = state.title.ifBlank {
                        "Unknown track"
                    },
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = state.artist.ifBlank {
                        "Unknown artist"
                    },
                    color = PlayerMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(22.dp))

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
                    inactiveTrackColor = Color(0xFF393142)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatPlayerTime(currentPosition.toLong()),
                    color = PlayerMuted,
                    fontSize = 11.sp
                )

                Text(
                    formatPlayerTime(duration.toLong()),
                    color = PlayerMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = PlayerLavender,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.width(24.dp))

                Surface(
                    onClick = {
                        musicPlayer.togglePlayPause()
                    },
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = PlayerPurple,
                    contentColor = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying)
                                Icons.Default.Pause
                            else
                                Icons.Default.PlayArrow,
                            contentDescription = "Play or pause",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next track",
                        tint = PlayerLavender,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(Modifier.weight(0.7f))

            Text(
                "FEEL THE SOUND",
                color = PlayerLavender.copy(alpha = 0.65f),
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 17.dp)
            )
        }
    }
}

// TIME FORMATTER

private fun formatPlayerTime(milliseconds: Long): String {
    val totalSeconds =
        (milliseconds / 1000).coerceAtLeast(0)

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

    val shape = RoundedCornerShape(26.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF292333),
                        Color(0xFF1C1825),
                        Color(0xFF15121C)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                shape
            )
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
                    .clickable {
                        onSelect(item.first)
                    }
                    .background(
                        if (isSelected)
                            Color(0xFF40334F).copy(alpha = 0.85f)
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

                Spacer(Modifier.height(5.dp))

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
