
package com.musicnova.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.InnerTubeMusicApi
import com.musicnova.app.data.music.LibraryStore
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.StreamExtraction
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.playback.MusicPlayer
import com.musicnova.app.playback.PlaybackState
import kotlinx.coroutines.launch

private val Black = Color(0xFF0D0B12)
private val Purple = Color(0xFF9277B8)
private val LightPurple = Color(0xFFC9B9E5)
private val CardColor = Color(0xFF1B1822)
private val MutedText = Color(0xFFAAA4B5)

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

    val playbackState by musicPlayer.playbackState.collectAsState()

    var homePlaybackError by remember {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(musicPlayer) {
        onDispose {
            musicPlayer.release()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Black,
            surface = CardColor,
            primary = Purple,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(28.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "MUSIC",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Light
                    )

                    Text(
                        " NOVA",
                        color = LightPurple,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.weight(1f))

                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = LightPurple,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                selectedTab = "Profile"
                            }
                    )
                }

                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 86.dp)
                ) {
                    when (selectedTab) {
                        "Home" -> {
                            Column {
                                homePlaybackError?.let { message ->
                                    Text(
                                        message,
                                        color = Color(0xFFFF9B9B),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                RecommendationsHomeScreen(
                                    onPlay = { song ->
                                        scope.launch {
                                            homePlaybackError = null

                                            try {
                                                val stream =
                                                    streamExtraction.resolve(song)

                                                musicPlayer.play(
                                                    streamUrl = stream.url,
                                                    title = song.title,
                                                    artist = song.artist,
                                                    artworkUrl = song.artworkUrl,
                                                    requestHeaders =
                                                        stream.requestHeaders
                                                )

                                                libraryStore.addRecentlyPlayed(
                                                    LibraryTrack(
                                                        videoId = song.videoId,
                                                        title = song.title,
                                                        artist = song.artist,
                                                        artworkUrl = song.artworkUrl
                                                    )
                                                )
                                            } catch (e: Exception) {
                                                homePlaybackError =
                                                    "Unable to play: ${e.message ?: "Try again"}"
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        "Search" -> SearchScreen(
                            musicPlayer = musicPlayer,
                            libraryStore = libraryStore
                        )

                        "Library" -> SimpleScreen("Your Library")

                        "Profile" -> SimpleScreen("Your Profile")
                    }
                }
            }

            if (playbackState.hasMedia) {
                GlassMiniPlayer(
                    state = playbackState,
                    onPlayPause = {
                        musicPlayer.togglePlayPause()
                    },
                    onStop = {
                        musicPlayer.stop()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 18.dp,
                            end = 18.dp,
                            bottom = 18.dp
                        )
                )
            } else {
                FlatNavigation(
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        )
                )
            }
        }
    }
}

@Composable
fun GlassMiniPlayer(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(23.dp),
                ambientColor = Purple.copy(alpha = 0.22f),
                spotColor = Purple.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(23.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xE52D2738),
                        Color(0xE51B1923),
                        Color(0xE52A2434)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.34f),
                        LightPurple.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                RoundedCornerShape(23.dp)
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!state.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = state.artworkUrl,
                contentDescription = "Now playing artwork",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Purple.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = LightPurple
                )
            }
        }

        Spacer(Modifier.width(11.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                state.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                state.artist.ifBlank { "Unknown artist" },
                color = LightPurple,
                fontSize = 11.sp,
                maxLines = 1
            )

            Text(
                if (state.isPlaying) "NOW PLAYING" else "PAUSED",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 8.sp,
                letterSpacing = 1.sp
            )
        }

        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = if (state.isPlaying) {
                    Icons.Default.PauseCircleFilled
                } else {
                    Icons.Default.PlayCircleFilled
                },
                contentDescription = "Play or pause",
                tint = Color.White,
                modifier = Modifier.size(31.dp)
            )
        }

        IconButton(
            onClick = onStop,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                Icons.Default.StopCircle,
                contentDescription = "Stop song",
                tint = LightPurple,
                modifier = Modifier.size(29.dp)
            )
        }
    }
}

@Composable
fun SearchScreen(
    musicPlayer: MusicPlayer,
    libraryStore: LibraryStore
) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var results by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }

    var loadingVideoId by remember {
        mutableStateOf<String?>(null)
    }

    val api = remember { InnerTubeMusicApi() }
    val streamExtraction = remember { StreamExtraction() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Search Music",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search songs, artists...")
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Purple,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (query.isNotBlank() && !loading) {
                    scope.launch {
                        loading = true
                        error = null
                        results = emptyList()

                        try {
                            results = api.searchSongs(query)
                        } catch (e: Exception) {
                            error = e.message ?: "Search failed"
                        } finally {
                            loading = false
                        }
                    }
                }
            },
            enabled = query.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Searching..." else "Search")
        }

        Spacer(Modifier.height(18.dp))

        if (loading) {
            CircularProgressIndicator(color = LightPurple)
        }

        error?.let {
            Text(it, color = Color(0xFFFF9B9B), fontSize = 13.sp)
        }

        if (results.isEmpty() && !loading && error == null) {
            Text(
                "Search songs and artists from YouTube Music",
                color = LightPurple,
                fontSize = 13.sp
            )
        }

        results.forEach { item ->
            SongSearchItem(
                song = item,
                isLoading = loadingVideoId == item.videoId,
                onPlay = {
                    if (!item.isArtist && loadingVideoId == null) {
                        scope.launch {
                            loadingVideoId = item.videoId

                            try {
                                val stream = streamExtraction.resolve(item)

                                musicPlayer.play(
                                    streamUrl = stream.url,
                                    title = item.title,
                                    artist = item.artist,
                                    artworkUrl = item.artworkUrl,
                                    requestHeaders = stream.requestHeaders
                                )

                                libraryStore.addRecentlyPlayed(
                                    LibraryTrack(
                                        videoId = item.videoId,
                                        title = item.title,
                                        artist = item.artist,
                                        artworkUrl = item.artworkUrl
                                    )
                                )
                            } catch (e: Exception) {
                                error = "Unable to play: ${e.message}"
                            } finally {
                                loadingVideoId = null
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SongSearchItem(
    song: YouTubeMusicTrack,
    isLoading: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!song.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = if (song.isArtist) {
                    "Artist profile picture"
                } else {
                    "Song cover"
                },
                modifier = Modifier
                    .size(58.dp)
                    .clip(
                        if (song.isArtist) CircleShape
                        else RoundedCornerShape(12.dp)
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(
                        if (song.isArtist) CircleShape
                        else RoundedCornerShape(12.dp)
                    )
                    .background(CardColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (song.isArtist) {
                        Icons.Default.Person
                    } else {
                        Icons.Default.MusicNote
                    },
                    contentDescription = null,
                    tint = LightPurple,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.width(13.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                if (song.isArtist) "ARTIST" else song.artist,
                color = LightPurple,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        if (song.isArtist) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Artist",
                tint = LightPurple,
                modifier = Modifier.size(22.dp)
            )
        } else {
            IconButton(
                onClick = onPlay,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(23.dp),
                        color = LightPurple,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        contentDescription = "Play song",
                        tint = LightPurple,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleScreen(title: String) {
    Column {
        Text(
           
