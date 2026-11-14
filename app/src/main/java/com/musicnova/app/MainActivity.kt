package com.musicnova.app

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil.compose.AsyncImage
import com.musicnova.app.data.music.LibraryStore
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.StreamExtraction
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.PlaybackQueue
import com.musicnova.app.ui.home.HomeSong
import com.musicnova.app.ui.home.MusicNovaShell
import com.musicnova.app.ui.home.PlayerActions
import com.musicnova.app.ui.home.PlayerUiState
import com.musicnova.app.ui.theme.NovaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NovaBlack = Color(0xFF09080E)
private val NovaSurface = Color(0xFF191620)
private val NovaPurple = Color(0xFF9277B8)
private val NovaLavender = Color(0xFFC9B9E5)
private val NovaMuted = Color(0xFFAAA4B5)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            MusicNovaApp()
        }
    }
}

@Composable
fun MusicNovaApp() {

    var selectedTab by remember {
        mutableStateOf("Home")
    }

    var showFullPlayer by remember {
        mutableStateOf(false)
    }

    var showQueueScreen by remember {
        mutableStateOf(false)
    }

    var currentBrowseId by remember {
        mutableStateOf<String?>(null)
    }

    var playbackError by remember {
        mutableStateOf<String?>(null)
    }

    var activeTrack by remember {
        mutableStateOf<YouTubeMusicTrack?>(null)
    }

    var fallbackAttemptedVideoId by remember {
        mutableStateOf<String?>(null)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val musicPlayer = remember(context) {
        MusicPlayer(context)
    }

    val libraryStore = remember(context) {
        LibraryStore(context)
    }

    val streamExtraction = remember {
        StreamExtraction()
    }

    val playbackQueue = remember {
        PlaybackQueue()
    }

    val playbackState by musicPlayer.playbackState.collectAsState()

    val queueState by playbackQueue.state.collectAsState()

    var positionMs by remember {
        mutableLongStateOf(0L)
    }

    var durationMs by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(playbackState.hasMedia) {

        while (playbackState.hasMedia) {
            positionMs = musicPlayer.getCurrentPosition()
            durationMs = musicPlayer.getDuration()
            delay(500)
        }

        positionMs = 0L
        durationMs = 0L
    }

    val glassPlayerState = PlayerUiState(
        song = if (playbackState.hasMedia) {
            HomeSong(
                id = activeTrack?.videoId ?: playbackState.title,
                title = playbackState.title,
                artist = playbackState.artist,
                artworkUrl = playbackState.artworkUrl,
                isLiked = activeTrack?.let {
                    libraryStore.isLiked(it.videoId)
                } ?: false
            )
        } else {
            null
        },
        isPlaying = playbackState.isPlaying,
        isBuffering = false,
        positionMs = positionMs,
        durationMs = durationMs
    )

    DisposableEffect(musicPlayer) {
        onDispose {
            musicPlayer.release()
        }
    }

    DisposableEffect(musicPlayer, streamExtraction) {

        musicPlayer.setOnPlaybackErrorListener { error ->

            val track = activeTrack

            if (track == null) {

                playbackError =
                    "Playback failed: ${error.message ?: "Please try again"}"

            } else if (fallbackAttemptedVideoId == track.videoId) {

                playbackError =
                    "Playback failed: ${error.message ?: "Please try again"}"

            } else {

                fallbackAttemptedVideoId = track.videoId
                playbackError = "Stream failed. Trying backup..."

                scope.launch {
                    try {

                        val fallback =
                            streamExtraction.resolveWithNewPipe(track.videoId)

                        musicPlayer.play(
                            streamUrl = fallback.url,
                            title = track.title,
                            artist = track.artist,
                            artworkUrl = track.artworkUrl,
                            requestHeaders = fallback.requestHeaders
                        )

                        playbackError = null

                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {

                        playbackError =
                            "Playback failed: ${e.message ?: "Please try again"}"
                    }
                }
            }
        }

        onDispose {
            musicPlayer.setOnPlaybackErrorListener(null)
        }
    }

    BackHandler(enabled = showQueueScreen) {
        showQueueScreen = false
    }

    BackHandler(enabled = showFullPlayer && !showQueueScreen) {
        showFullPlayer = false
    }

    BackHandler(
        enabled = currentBrowseId != null &&
            !showFullPlayer &&
            !showQueueScreen
    ) {
        currentBrowseId = null
    }

    BackHandler(
        enabled = selectedTab != "Home" &&
            !showFullPlayer &&
            !showQueueScreen &&
            currentBrowseId == null
    ) {
        selectedTab = "Home"
    }
    
    fun playTrack(track: YouTubeMusicTrack) {

        if (track.isArtist || track.videoId.isBlank()) return

        activeTrack = track
        fallbackAttemptedVideoId = null

        scope.launch {

            playbackError = null

            try {

                val stream = streamExtraction.resolve(track)

                musicPlayer.play(
                    streamUrl = stream.url,
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = track.artworkUrl,
                    requestHeaders = stream.requestHeaders
                )

                libraryStore.addRecentlyPlayed(
                    track.toLibraryTrack()
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                playbackError =
                    "Unable to play: ${e.message ?: "Please try again"}"
            }
        }
    }

    fun playQueue(
        tracks: List<YouTubeMusicTrack>,
        selectedTrack: YouTubeMusicTrack
    ) {

        playbackQueue.setQueue(
            tracks = tracks,
            startVideoId = selectedTrack.videoId
        )

        playbackQueue.peekNext()?.let { nextTrack ->
            streamExtraction.prefetch(nextTrack.videoId)
        }

        playTrack(selectedTrack)
    }

    fun playNextTrack() {

        val nextTrack = playbackQueue.next() ?: return

        playbackQueue.peekNext()?.let { track ->
            streamExtraction.prefetch(track.videoId)
        }

        playTrack(nextTrack)
    }

    fun playPreviousTrack() {

        val previousTrack = playbackQueue.previous() ?: return

        playTrack(previousTrack)
    }

    DisposableEffect(musicPlayer) {

        musicPlayer.setOnTrackEndedListener {
            playNextTrack()
        }

        onDispose {
            musicPlayer.setOnTrackEndedListener(null)
        }
    }

    val glassPlayerActions = PlayerActions(

        onTogglePlay = {
            musicPlayer.togglePlayPause()
        },

        onNext = {
            playNextTrack()
        },

        onPrevious = {
            playPreviousTrack()
        },

        onOpenPlayer = {
            showFullPlayer = true
        },

        onDismiss = {
            musicPlayer.stop()
            activeTrack = null
        },

        onToggleLike = {
            activeTrack?.let { track ->
                libraryStore.toggleLike(track.toLibraryTrack())
            }
        }
    )
    
    NovaTheme {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF100D17),
                            NovaBlack,
                            Color(0xFF17111F)
                        )
                    )
                )
        ) {

            if (showQueueScreen) {

                QueueScreen(
                    state = queueState,

                    onBack = {
                        showQueueScreen = false
                    },

                    onPlay = { track ->

                        playbackQueue.setQueue(
                            tracks = queueState.tracks,
                            startVideoId = track.videoId
                        )

                        showQueueScreen = false
                        playTrack(track)
                    },

                    onRemove = { index ->
                        playbackQueue.removeAt(index)
                    },

                    onMove = { from, to ->
                        playbackQueue.move(from, to)
                    },

                    onClear = {
                        playbackQueue.clear()
                    }
                )

            } else if (showFullPlayer && playbackState.hasMedia) {

                
                NovaFullPlayerScreen(
                    state = playbackState,
                    musicPlayer = musicPlayer,

                    onClose = {
                        showFullPlayer = false
                    },

                    onStop = {
                        musicPlayer.stop()
                        showFullPlayer = false
                    },

                    onNext = {
                        playNextTrack()
                    },

                    onPrevious = {
                        playPreviousTrack()
                    },

                    onOpenQueue = {
                        showQueueScreen = true
                    },

                    shuffleEnabled = queueState.shuffleEnabled,

                    repeatMode = queueState.repeatMode,

                    onToggleShuffle = {
                        val enabled = playbackQueue.toggleShuffle()
                        musicPlayer.setShuffleEnabled(enabled)
                    },

                    onCycleRepeat = {
                        val mode = playbackQueue.cycleRepeatMode()
                        musicPlayer.setRepeatMode(mode)
                    }
                )
                

                )

            } else {

                MusicNovaShell(
                    player = glassPlayerState,
                    playerActions = glassPlayerActions,
                    selectedRoute = selectedTab.lowercase(),

                    onNavigate = { route ->

                        currentBrowseId = null

                        selectedTab = when (route) {
                            "search" -> "Search"
                            "library" -> "Library"
                            "profile" -> "Profile"
                            else -> "Home"
                        }
                    },

                    onReselect = { route ->

                        if (route == "home") {
                            currentBrowseId = null
                        }
                    }

                ) { shellPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = shellPadding.calculateTopPadding()
                            )
                    ) {

                        if (currentBrowseId == null) {

                            NovaTopHeader(
                                selectedTab = selectedTab,

                                onSearchClick = {
                                    selectedTab = "Search"
                                },

                                onProfileClick = {
                                    selectedTab = "Profile"
                                }
                            )
                        }

                        playbackError?.let { message ->

                            Text(
                                text = message,
                                color = Color(0xFFFF9999),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 8.dp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {

                            if (currentBrowseId != null) {

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            bottom = shellPadding.calculateBottomPadding()
                                        )
                                ) {

                                    BrowseScreen(
                                        browseId = currentBrowseId!!,

                                        onBack = {
                                            currentBrowseId = null
                                        },

                                        onPlay = { track ->
                                            playTrack(track)
                                        },

                                        onBrowse = { nextBrowseId ->
                                            currentBrowseId = nextBrowseId
                                        }
                                    )
                                }

                            } else {

                                AnimatedContent(
                                    targetState = selectedTab,
                                    label = "NovaScreenTransition"
                                ) { tab ->

                                    when (tab) {
                                        
                                        "Home" -> {

                                            RecommendationsHomeScreen(
                                                onPlay = { track ->
                                                    playTrack(track)
                                                },

                                                onPlayQueue = { tracks, selectedTrack ->
                                                    playQueue(tracks, selectedTrack)
                                                },

                                                onBrowse = { browseId ->
                                                    currentBrowseId = browseId
                                                },

                                                contentPadding = PaddingValues(
                                                    bottom = shellPadding.calculateBottomPadding()
                                                )
                                            )
                                        }

                                        "Search" -> {

                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        bottom = shellPadding.calculateBottomPadding()
                                                    )
                                            ) {

                                                NovaSearchScreen(
                                                    libraryStore = libraryStore,

                                                    onPlay = { track ->
                                                        playTrack(track)
                                                    },

                                                    onPlayQueue = { tracks, selectedTrack ->
                                                        playQueue(tracks, selectedTrack)
                                                    }
                                                )
                                            }
                                        }

                                        "Library" -> {

                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        bottom = shellPadding.calculateBottomPadding()
                                                    )
                                            ) {

                                                LibraryScreen(
                                                    libraryStore = libraryStore,

                                                    onPlay = { track ->
                                                        playTrack(track.toMusicTrack())
                                                    },

                                                    onPlayQueue = { tracks, selectedTrack ->

                                                        playQueue(
                                                            tracks = tracks.map {
                                                                it.toMusicTrack()
                                                            },
                                                            selectedTrack = selectedTrack.toMusicTrack()
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        "Profile" -> {

                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        bottom = shellPadding.calculateBottomPadding()
                                                    )
                                            ) {
                                                NovaProfileScreen()
                                            }
                                        }

                                        else -> {

                                            RecommendationsHomeScreen(
                                                onPlay = { track ->
                                                    playTrack(track)
                                                },

                                                onPlayQueue = { tracks, selectedTrack ->
                                                    playQueue(tracks, selectedTrack)
                                                },

                                                onBrowse = { browseId ->
                                                    currentBrowseId = browseId
                                                },

                                                contentPadding = PaddingValues(
                                                    bottom = shellPadding.calculateBottomPadding()
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaTopHeader(
    selectedTab: String,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {

    val context = LocalContext.current

    val profilePicture = remember(selectedTab) {
        context.getSharedPreferences("nova_profile", 0)
            .getString("profile_picture", "builtin:person")
            ?: "builtin:person"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 22.dp,
                end = 14.dp,
                top = 10.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {

            Text(
                text = "MUSIC NOVA",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = selectedTab.uppercase(),
                color = NovaMuted,
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onSearchClick) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = NovaLavender,
                modifier = Modifier.size(27.dp)
            )
        }

        Spacer(Modifier.width(5.dp))

        IconButton(onClick = onProfileClick) {

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF30263D)),
                contentAlignment = Alignment.Center
            ) {

                if (profilePicture.startsWith("builtin:")) {

                    val avatarIcon = when (
                        profilePicture.removePrefix("builtin:")
                    ) {
                        "music" -> Icons.Default.MusicNote
                        "headphones" -> Icons.Default.Headphones
                        "album" -> Icons.Default.Album
                        "equalizer" -> Icons.Default.Equalizer
                        "favorite" -> Icons.Default.Favorite
                        else -> Icons.Default.Person
                    }

                    Icon(
                        imageVector = avatarIcon,
                        contentDescription = "Profile",
                        tint = NovaLavender,
                        modifier = Modifier.size(25.dp)
                    )

                } else {

                    AsyncImage(
                        model = Uri.parse(profilePicture),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun YouTubeMusicTrack.toLibraryTrack(): LibraryTrack {

    return LibraryTrack(
        videoId = videoId,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl
    )
}

private fun LibraryTrack.toMusicTrack(): YouTubeMusicTrack {

    return YouTubeMusicTrack(
        videoId = videoId,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        isArtist = false
    )
}
