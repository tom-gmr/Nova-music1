package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.PlaybackState
import com.musicnova.app.playback.AudioSignalPathScreen
import com.musicnova.app.data.lyrics.LyricsRepository
import com.musicnova.app.data.lyrics.LyricsResult
import com.musicnova.app.data.lyrics.parseLyrics
import com.musicnova.app.ui.player.LyricsUiState
import com.musicnova.app.ui.player.KaraokeLyricText
import kotlinx.coroutines.delay

private val PlayerBackground = Color(0xFF09070F)
private val PlayerSurface = Color(0xFF211B2B)
private val PlayerPurple = Color(0xFF9277B8)
private val PlayerLavender = Color(0xFFD1C1EB)
private val PlayerMuted = Color(0xFFAAA4B5)

/* ---------------- MINI PLAYER ---------------- */

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
    val shape = RoundedCornerShape(60.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable { onExpand() },
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 18.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.30f)
        )
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xF20B1111),
                            Color(0xDD38203D),
                            Color(0xCC9B5265),
                            Color(0xE51B1420)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = state.artworkUrl,
                contentDescription = "Album artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.title.ifBlank { "Not Playing" },
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = state.artist,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onPlayPause,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color(0xFFE8C9C9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (state.isPlaying)
                            Icons.Default.Pause
                        else
                            Icons.Default.PlayArrow,
                        contentDescription = "Play or pause",
                        tint = Color(0xFF321D25),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Surface(
                onClick = onNext,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color(0xFF382A30)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next track",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
/* ---------------- FULL PLAYER ---------------- */

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
    var isSeeking by remember { mutableStateOf(false) }
    var showSignalPath by remember { mutableStateOf(false) }

    var lyricsState by remember {
        mutableStateOf<LyricsUiState>(LyricsUiState.Idle)
    }

    val lyricsRepository = remember { LyricsRepository() }

    // Playback position update
    LaunchedEffect(musicPlayer) {
        while (true) {
            val total = musicPlayer.getDuration()
            val current = musicPlayer.getCurrentPosition()

            duration = if (total > 0L) {
                total.toFloat()
            } else {
                1f
            }

            if (!isSeeking) {
                currentPosition = current
                    .coerceAtLeast(0L)
                    .toFloat()
            }

            delay(400)
        }
    }

    // Fetch lyrics for the currently playing song
LaunchedEffect(state.title, state.artist) {
    if (state.title.isBlank() || state.artist.isBlank()) {
        lyricsState = LyricsUiState.Empty
    } else {
        lyricsState = LyricsUiState.Loading

        try {
            var trackDuration = musicPlayer.getDuration()
            var attempts = 0

            // Wait up to 6 seconds for the actual track duration
            while (trackDuration <= 0L && attempts < 20) {
                delay(300)
                trackDuration = musicPlayer.getDuration()
                attempts++
            }

            val result = lyricsRepository.findLyrics(
                artist = state.artist,
                track = state.title,
                durationMs = trackDuration
            )

            lyricsState = if (result == null) {
                LyricsUiState.Empty
            } else {
                when (
                    val parsed = parseLyrics(
                        syncedLyrics = result.syncedLyrics,
                        plainLyrics = result.plainLyrics,
                        source = "LRCLIB"
                    )
                ) {
                    is LyricsResult.Success ->
                        LyricsUiState.Success(parsed)

                    LyricsResult.Empty ->
                        LyricsUiState.Empty

                    is LyricsResult.Error ->
                        LyricsUiState.Error(parsed.message)
                }
            }
        } catch (exception: Exception) {
            lyricsState = LyricsUiState.Error(
                exception.message ?: "Unable to load lyrics"
            )
        }
    }
}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBackground)
    ) {
        // Artwork background
        AsyncImage(
            model = state.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.TopCenter)
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55000000),
                            Color(0xAA09070F),
                            PlayerBackground,
                            PlayerBackground
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
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

            Spacer(Modifier.height(28.dp))

            // Static album artwork (rotation removed)
            Box(
                modifier = Modifier
                    .size(292.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        Color.White.copy(alpha = 0.28f),
                        CircleShape
                    )
            ) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.13f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.18f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f))
                        .border(
                            3.dp,
                            Color.Black.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
            }

            Spacer(Modifier.height(35.dp))
                        // Song information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title.ifBlank { "Unknown Track" },
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        text = state.artist.ifBlank { "Unknown Artist" },
                        color = PlayerMuted,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = PlayerPurple.copy(alpha = 0.20f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        PlayerLavender.copy(alpha = 0.25f)
                    )
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Now playing",
                        tint = PlayerLavender,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(23.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Audio Signal Path entry
            Surface(
                onClick = { showSignalPath = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = PlayerSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    PlayerLavender.copy(alpha = 0.22f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Audio Signal Path",
                        tint = PlayerLavender
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Audio Details • Signal Path",
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open Signal Path",
                        tint = PlayerLavender
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Seekbar
            Slider(
                value = currentPosition.coerceIn(
                    0f,
                    duration.coerceAtLeast(1f)
                ),
                onValueChange = {
                    isSeeking = true
                    currentPosition = it
                },
                onValueChangeFinished = {
                    musicPlayer.seekTo(currentPosition.toLong())
                    isSeeking = false
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

            // Playback controls
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
                        contentDescription = "Previous",
                        tint = PlayerLavender,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.width(28.dp))

                Surface(
                    onClick = { musicPlayer.togglePlayPause() },
                    modifier = Modifier.size(78.dp),
                    shape = CircleShape,
                    color = PlayerPurple,
                    shadowElevation = 16.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = "Play or pause",
                            tint = Color.White,
                            modifier = Modifier.size(43.dp)
                        )
                    }
                }

                Spacer(Modifier.width(28.dp))

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = PlayerLavender,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            
            // ---------------- LYRICS SECTION ----------------

            Spacer(Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LYRICS",
                        color = PlayerLavender,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Feel every word",
                        color = PlayerMuted,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PlayerPurple.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = when (val currentState = lyricsState) {
                            is LyricsUiState.Success -> {
                                if (currentState.result.isWordSynced) {
                                    "WORD SYNC"
                                } else if (currentState.result.isSynced) {
                                    "LINE SYNC"
                                } else {
                                    "PLAIN"
                                }
                            }
                            LyricsUiState.Loading -> "LOADING"
                            LyricsUiState.Empty -> "NO LYRICS"
                            is LyricsUiState.Error -> "ERROR"
                            LyricsUiState.Idle -> "WAITING"
                        },
                        color = PlayerLavender,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            when (val currentLyrics = lyricsState) {

                LyricsUiState.Idle,
                LyricsUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(PlayerSurface.copy(alpha = 0.75f))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = PlayerLavender,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Finding lyrics...",
                            color = PlayerMuted,
                            fontSize = 14.sp
                        )
                    }
                }

                LyricsUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(PlayerSurface.copy(alpha = 0.75f))
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.MusicOff,
                            contentDescription = null,
                            tint = PlayerLavender,
                            modifier = Modifier.size(34.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Lyrics not available",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = "Try another track",
                            color = PlayerMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                is LyricsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(PlayerSurface.copy(alpha = 0.75f))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = PlayerLavender,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = currentLyrics.message,
                            color = PlayerMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                is LyricsUiState.Success -> {
                    val result = currentLyrics.result
                    val lines = result.lines

                    if (result.isInstrumental) {
                        Text(
                            text = "♪ Instrumental ♪",
                            color = PlayerLavender,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 35.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else if (result.isSynced) {
                        val activeIndex = lines.indexOfLast {
                            it.timeMs <= currentPosition.toLong()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF292035),
                                            Color(0xFF17121F)
                                        )
                                    )
                                )
                                .padding(horizontal = 18.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            lines.forEachIndexed { index, line ->
                                val isActive = index == activeIndex

                                if (index in (activeIndex - 1)..(activeIndex + 2)) {
                                    KaraokeLyricText(
                                        line = line,
                                        currentPositionMs =
                                            currentPosition.toLong(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                musicPlayer.seekTo(line.timeMs)
                                                currentPosition =
                                                    line.timeMs.toFloat()
                                            }
                                            .padding(
                                                horizontal = 10.dp,
                                                vertical = if (isActive) {
                                                    17.dp
                                                } else {
                                                    12.dp
                                                }
                                            ),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = if (isActive) {
                                                21.sp
                                            } else {
                                                16.sp
                                            },
                                            fontWeight = if (isActive) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Medium
                                            }
                                        ),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        activeColor = PlayerLavender,
                                        sungColor = Color.White,
                                        upcomingColor = PlayerMuted
                                    )
                                }
                            }
                        }
                    } else {
                        // Plain lyrics: show all lines
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(PlayerSurface.copy(alpha = 0.75f))
                                .padding(20.dp)
                        ) {
                            lines.forEach { line ->
                                Text(
                                    text = line.text,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 16.sp,
                                    lineHeight = 27.sp,
                                    modifier = Modifier.padding(
                                        vertical = 7.dp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Lyrics source: ${result.source ?: "Unknown"}",
                        color = PlayerMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            // Footer
            Spacer(Modifier.height(20.dp))

            Text(
                text = "FEEL THE SOUND",
                color = PlayerLavender.copy(alpha = 0.75f),
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 22.dp)
            )
        }

        // Signal Path full-screen dialog
        if (showSignalPath) {
            Dialog(
                onDismissRequest = {
                    showSignalPath = false
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PlayerBackground
                ) {
                    AudioSignalPathScreen(
                        onBack = {
                            showSignalPath = false
                        }
                    )
                }
            }
        }
    }
}

/* ---------------- TIME FORMATTER ---------------- */

private fun formatPlayerTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)

    return "%d:%02d".format(
        totalSeconds / 60,
        totalSeconds % 60
    )
}

/* ---------------- FLOATING BOTTOM NAVIGATION ---------------- */

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
                    listOf(
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
            .padding(
                horizontal = 7.dp,
                vertical = 8.dp
            ),
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
                        if (isSelected) {
                            Color(0xFF40334F).copy(alpha = 0.85f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.second,
                    contentDescription = item.third,
                    tint = if (isSelected) {
                        PlayerLavender
                    } else {
                        PlayerMuted
                    },
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = item.first,
                    color = if (isSelected) {
                        PlayerLavender
                    } else {
                        PlayerMuted
                    },
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
                )
            }
        }
    }
}
