package com.musicnova.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.PlaybackState
import com.musicnova.app.playback.RepeatMode
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
                        imageVector = if (state.isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
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
    onPrevious: () -> Unit,
    onOpenQueue: () -> Unit = {},
    shuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {}
) {
    var currentPosition by remember {
        mutableFloatStateOf(0f)
    }

    var duration by remember {
        mutableFloatStateOf(1f)
    }

    var isSeeking by remember {
        mutableStateOf(false)
    }

    var showSignalPath by remember {
        mutableStateOf(false)
    }

    var showLyricsScreen by remember {
        mutableStateOf(false)
    }

    var showMenu by remember {
        mutableStateOf(false)
    }

    val lyricsRepository = remember {
        LyricsRepository()
    }

    var lyricsState by remember {
        mutableStateOf<LyricsUiState>(LyricsUiState.Idle)
    }

    // Artwork position for exact gradient start

    var artworkBottomPx by remember {
        mutableFloatStateOf(0f)
    }

    // Dynamic artwork palette extraction

    val artworkContext = LocalContext.current

    var artworkPalette by remember(state.artworkUrl) {
        mutableStateOf<NovaArtworkPalette?>(null)
    }

    LaunchedEffect(state.artworkUrl) {
        artworkPalette = null
        artworkBottomPx = 0f

        val url = state.artworkUrl.orEmpty()

        if (url.isNotBlank()) {
            artworkPalette = extractNovaArtworkPalette(
                context = artworkContext,
                artworkUrl = url
            )
        }
    }

    // Smooth artwork colour transitions

    val ambientPrimary by animateColorAsState(
        targetValue = artworkPalette?.primary
            ?: Color(0xFF49345F),
        animationSpec = tween(durationMillis = 700),
        label = "ArtworkPrimary"
    )

    val ambientSecondary by animateColorAsState(
        targetValue = artworkPalette?.secondary
            ?: Color(0xFF352B50),
        animationSpec = tween(durationMillis = 700),
        label = "ArtworkSecondary"
    )

    val ambientTertiary by animateColorAsState(
        targetValue = artworkPalette?.tertiary
            ?: Color(0xFF241D39),
        animationSpec = tween(durationMillis = 700),
        label = "ArtworkTertiary"
    )
        // Playback position synchronization

    LaunchedEffect(musicPlayer) {
        while (true) {
            val trackDuration = musicPlayer.getDuration()
            val trackPosition = musicPlayer.getCurrentPosition()

            if (trackDuration > 0L) {
                duration = trackDuration.toFloat()
            }

            if (!isSeeking) {
                currentPosition = trackPosition.toFloat()
            }

            delay(400)
        }
    }

    // Lyrics loading

    LaunchedEffect(state.title, state.artist) {
        if (state.title.isBlank() || state.artist.isBlank()) {
            lyricsState = LyricsUiState.Empty
        } else {
            lyricsState = LyricsUiState.Loading

            try {
                var trackDuration = musicPlayer.getDuration()
                var attempts = 0

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

    // Main player background and artwork-based gradient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBackground)
            .drawBehind {
                if (
                    artworkBottomPx > 0f &&
                    size.height > artworkBottomPx
                ) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to ambientPrimary.copy(alpha = 0.62f),
                                0.34f to ambientSecondary.copy(alpha = 0.38f),
                                0.70f to ambientTertiary.copy(alpha = 0.18f),
                                1.0f to PlayerBackground
                            ),
                            startY = artworkBottomPx,
                            endY = size.height
                        )
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {

            // Top bar

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NOW PLAYING",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "Music Nova",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White,
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Audio signal path") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                showSignalPath = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Lyrics") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lyrics,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                showLyricsScreen = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Queue") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.QueueMusic,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onOpenQueue()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Album artwork area
            // Gradient begins at the exact bottom edge of this image.

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = "Current album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .onGloballyPositioned { coordinates ->
                            artworkBottomPx =
                                coordinates.positionInRoot().y +
                                coordinates.size.height
                        }
                )
            }

            Spacer(Modifier.height(12.dp))
                    // Track information

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.title.ifBlank { "Unknown Track" },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = state.artist.ifBlank { "Unknown Artist" },
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(20.dp))

            // Playback progress

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = currentPosition.coerceIn(
                        0f,
                        duration.coerceAtLeast(1f)
                    ),
                    onValueChange = { value ->
                        isSeeking = true
                        currentPosition = value
                    },
                    onValueChangeFinished = {
                        musicPlayer.seekTo(currentPosition.toLong())
                        isSeeking = false
                    },
                    valueRange = 0f..duration.coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = PlayerLavender,
                        activeTrackColor = PlayerLavender,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatPlayerTime(currentPosition.toLong()),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )

                    Text(
                        text = formatPlayerTime(duration.toLong()),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Main playback controls

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) {
                            PlayerLavender
                        } else {
                            Color.White.copy(alpha = 0.55f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Surface(
                    onClick = {
                        musicPlayer.togglePlayPause()
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = PlayerLavender
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (state.isPlaying) {
                                "Pause"
                            } else {
                                "Play"
                            },
                            tint = PlayerBackground,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next track",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat mode",
                        tint = if (repeatMode != RepeatMode.OFF) {
                            PlayerLavender
                        } else {
                            Color.White.copy(alpha = 0.55f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Bottom player actions

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showLyricsScreen = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = null,
                        tint = PlayerLavender
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "Lyrics",
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = onOpenQueue
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = PlayerLavender
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "Queue",
                        color = Color.White
                    )
                }
            }
                    } // Main Column
    } // Root Box

    // Audio Signal Path dialog

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

    // Dedicated Lyrics dialog

    if (showLyricsScreen) {
        Dialog(
            onDismissRequest = {
                showLyricsScreen = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = PlayerBackground
            ) {
                NovaDedicatedLyricsScreen(
                    title = state.title,
                    artist = state.artist,
                    lyricsState = lyricsState,
                    currentPosition = currentPosition,
                    onBack = {
                        showLyricsScreen = false
                    },
                    onSeek = { position ->
                        musicPlayer.seekTo(position)
                        currentPosition = position.toFloat()
                    }
                )
            }
        }
    }
} // NovaFullPlayerScreen
/* ---------------- DEDICATED LYRICS SCREEN ---------------- */

@Composable
fun NovaDedicatedLyricsScreen(
    title: String,
    artist: String,
    lyricsState: LyricsUiState,
    currentPosition: Float,
    onBack: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val scrollState = rememberScrollState()

    val activeIndex = remember(lyricsState, currentPosition) {
        val success = lyricsState as? LyricsUiState.Success

        success?.result?.lines?.indexOfLast {
            it.timeMs <= currentPosition.toLong()
        } ?: -1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back to player",
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LYRICS",
                    color = PlayerLavender,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Text(
                    "Now playing",
                    color = PlayerMuted,
                    fontSize = 11.sp
                )
            }

            Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                tint = PlayerLavender,
                modifier = Modifier.size(25.dp)
            )
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.10f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Text(
                title.ifBlank { "Unknown Track" },
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            Text(
                artist.ifBlank { "Unknown Artist" },
                color = PlayerMuted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val currentLyrics = lyricsState) {
                LyricsUiState.Idle,
                LyricsUiState.Loading -> {
                    Spacer(Modifier.height(100.dp))

                    CircularProgressIndicator(
                        color = PlayerLavender,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(38.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Finding lyrics...",
                        color = PlayerMuted,
                        fontSize = 15.sp
                    )
                }

                LyricsUiState.Empty -> {
                    Spacer(Modifier.height(100.dp))

                    Icon(
                        Icons.Default.MusicOff,
                        contentDescription = null,
                        tint = PlayerLavender,
                        modifier = Modifier.size(45.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Lyrics not available",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        "Try another track",
                        color = PlayerMuted,
                        fontSize = 13.sp
                    )
                }

                is LyricsUiState.Error -> {
                    Spacer(Modifier.height(100.dp))

                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = PlayerLavender,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.height(15.dp))

                    Text(
                        currentLyrics.message,
                        color = PlayerMuted,
                        fontSize = 14.sp
                    )
                }

                is LyricsUiState.Success -> {
                    val result = currentLyrics.result
                    val lines = result.lines

                    if (result.isInstrumental) {
                        Spacer(Modifier.height(100.dp))

                        Text(
                            "♪ Instrumental ♪",
                            color = PlayerLavender,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (result.isSynced) {
                        Spacer(Modifier.height(25.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            PlayerBackground,
                                            PlayerSurface,
                                            PlayerBackground
                                        )
                                    )
                                )
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 18.dp
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            lines.forEachIndexed { index, line ->
                                val isActive = index == activeIndex

                                if (
                                    index in
                                    (activeIndex - 2)..(activeIndex + 3)
                                ) {
                                    KaraokeLyricText(
                                        line = line,
                                        currentPositionMs =
                                            currentPosition.toLong(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                onSeek(line.timeMs)
                                            }
                                            .padding(
                                                horizontal = 10.dp,
                                                vertical = if (isActive) {
                                                    18.dp
                                                } else {
                                                    13.dp
                                                }
                                            ),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = if (isActive) {
                                                22.sp
                                            } else {
                                                16.sp
                                            },
                                            fontWeight = if (isActive) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Medium
                                            }
                                        ),
                                        textAlign =
                                            androidx.compose.ui.text.style.TextAlign.Center,
                                        activeColor = PlayerLavender,
                                        sungColor = Color.White,
                                        upcomingColor = PlayerMuted
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(15.dp))

                        Text(
                            "Tap any lyric line to jump to that part",
                            color = PlayerMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    PlayerSurface.copy(alpha = 0.75f)
                                )
                                .padding(20.dp)
                        ) {
                            lines.forEach { line ->
                                Text(
                                    text = line.text,
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 17.sp,
                                    lineHeight = 29.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "Lyrics source: ${result.source ?: "Unknown"}",
                        color = PlayerMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            Spacer(Modifier.height(35.dp))

            Text(
                "FEEL THE SOUND",
                color = PlayerLavender.copy(alpha = 0.75f),
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(bottom = 25.dp)
            )
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
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = selectedTab == item.first

            TextButton(
                onClick = { onSelect(item.first) }
            ) {
                Column(
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
                        modifier = Modifier.size(23.dp)
                    )

                    Spacer(Modifier.height(3.dp))

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
}
