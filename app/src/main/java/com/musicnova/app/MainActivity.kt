
package com.musicnova.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicnova.app.data.music.LibraryStore
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.StreamExtraction
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.PlaybackState
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.musicnova.app.playback.PlaybackQueue
private val NovaBlack = Color(0xFF09080E)
private val NovaSurface = Color(0xFF191620)
private val NovaPurple = Color(0xFF9277B8)
private val NovaLavender = Color(0xFFC9B9E5)
private val NovaMuted = Color(0xFFAAA4B5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MusicNovaApp()
        }
    }
}

@Composable
fun MusicNovaApp() {
    var selectedTab by remember { mutableStateOf("Home") }
    var showFullPlayer by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
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
    } val playbackQueue = remember {
    PlaybackQueue()
    }

    val playbackState by musicPlayer.playbackState.collectAsState()
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
        
    

    BackHandler(
        enabled = showFullPlayer
    ) {
        showFullPlayer = false
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
            } catch (e: Exception) {
                playbackError =
                    "Unable to play: ${e.message ?: "Please try again"}"
            } fun playQueue(
    tracks: List<YouTubeMusicTrack>,
    selectedTrack: YouTubeMusicTrack
) {
    playbackQueue.setQueue(
        tracks = tracks,
        startVideoId = selectedTrack.videoId
    )

    playTrack(selectedTrack)
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NovaPurple,
            secondary = NovaLavender,
            background = NovaBlack,
            surface = NovaSurface,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
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
            if (showFullPlayer && playbackState.hasMedia) {

                NovaFullPlayerScreen(
                    state = playbackState,
                    musicPlayer = musicPlayer,
                    onClose = {
                        showFullPlayer = false
                    },
                    onStop = {
                        musicPlayer.stop()
                        showFullPlayer = false
                    }
                )

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {

                    if (selectedTab != "Home") {
                        NovaTopHeader(
                            selectedTab = selectedTab,
                            onProfileClick = {
                                selectedTab = "Profile"
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 88.dp)
                    ) {
                        Column(
                   modifier = Modifier.fillMaxSize()
                  ) {
                        

                            playbackError?.let { message ->
                                Text(
                                    text = message,
                                    color = Color(0xFFFF9999),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(
                                        bottom = 8.dp
                                    )
                                )
                            }

                            AnimatedContent(
                                targetState = selectedTab,
                                label = "NovaScreenTransition"
                            ) { tab ->

                                when (tab) {
                                    "Home" -> {
                                        RecommendationsHomeScreen(
                                            onPlay = { track ->
                                                playTrack(track)
                                            }
                                        )
                                    }

                                    "Search" -> {
                                        NovaSearchScreen(
                                            libraryStore = libraryStore,
                                            onPlay = { track ->
                                                playTrack(track)
                                            }
                                        )
                                    }

                                    "Library" -> {
                                        LibraryScreen(
                                            libraryStore = libraryStore,
                                            onPlay = { track ->
                                                playTrack(
                                                    track.toMusicTrack()
                                                )
                                            }
                                        )
                                    }

                                    "Profile" -> {
                                        NovaProfileScreen()
                                    }

                                    else -> {
                                        RecommendationsHomeScreen(
                                            onPlay = { track ->
                                                playTrack(track)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Playing song: mini-player replaces navigation.
                // No song: floating navigation is visible.
                if (playbackState.hasMedia) {
                    NovaGlassMiniPlayer(
                        state = playbackState,
                        onPlayPause = {
                            musicPlayer.togglePlayPause()
                        },
                        onStop = {
                            musicPlayer.stop()
                        },
                        onExpand = {
                            showFullPlayer = true
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            )
                    )
                } else {
                    NovaBottomNavigation(
                        selectedTab = selectedTab,
                        onSelect = { selectedTab = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaTopHeader(
    selectedTab: String,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 22.dp,
                end = 14.dp,
                top = 14.dp,
                bottom = 16.dp
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

        IconButton(
            onClick = onProfileClick
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile",
                tint = NovaLavender,
                modifier = Modifier.size(29.dp)
            )
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

