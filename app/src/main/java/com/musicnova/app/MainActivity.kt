
package com.musicnova.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.InnerTubeMusicApi
import com.musicnova.app.data.music.StreamExtraction
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.playback.MusicPlayer
import kotlinx.coroutines.launch

private val Black = Color(0xFF08070D)
private val Purple = Color(0xFF8B5CF6)
private val LightPurple = Color(0xFFC4B5FD)
private val CardColor = Color(0xFF191622)

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
    val musicPlayer = remember(context) {
        MusicPlayer(context)
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
            primary = Purple
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
                        tint = Color.White,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                selectedTab = "Profile"
                            }
                    )
                }

                Spacer(Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 85.dp)
                ) {
                    when (selectedTab) {
                        "Home" -> HomeScreen()
                        "Search" -> SearchScreen(musicPlayer)
                        "Library" -> SimpleScreen("Your Library")
                        "Profile" -> SimpleScreen("Your Profile")
                    }
                }
            }

            FloatingNavigation(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        bottom = 18.dp
                    )
            )
        }
    }
}

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "FEEL THE SOUND",
            color = Color.White,
            fontSize = 29.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Your music. Your universe.",
            color = LightPurple,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF5425A8),
                            Color(0xFF241044),
                            Color(0xFF111019)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    "YOUR WORLD.",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "YOUR MUSIC.",
                    color = LightPurple,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Discover your next favourite sound",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        Text(
            "Discover",
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DiscoverCard(
                "Trending",
                "Popular sounds",
                Modifier.weight(1f)
            )

            DiscoverCard(
                "New Releases",
                "Fresh music",
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(25.dp))

        Text(
            "Your Music Space",
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    CardColor,
                    RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Text(
                "Your music library and playlists will appear here.",
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DiscoverCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                CardColor,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = LightPurple,
            modifier = Modifier.size(27.dp)
        )

        Spacer(Modifier.height(13.dp))

        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            subtitle,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

@Composable
fun SearchScreen(musicPlayer: MusicPlayer) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var results by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }

    var loadingVideoId by remember {
        mutableStateOf<String?>(null)
    }

    var playingTitle by remember {
        mutableStateOf<String?>(null)
    }

    var playbackMessage by remember {
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
                Icon(
                    Icons.Default.Search,
                    contentDescription = null
                )
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
            Text(
                if (loading) "Searching..." else "Search"
            )
        }

        Spacer(Modifier.height(18.dp))

        if (loading) {
            CircularProgressIndicator(
                color = LightPurple
            )
        }

        error?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 13.sp
            )
        }

        if (results.isEmpty() && !loading && error == null) {
            Text(
                "Search songs and artists from YouTube Music",
                color = LightPurple,
                fontSize = 13.sp
            )
        }

        if (playingTitle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        CardColor,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = LightPurple
                )

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        "Now playing",
                        color = LightPurple,
                        fontSize = 11.sp
                    )

                    Text(
                        playingTitle.orEmpty(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        playbackMessage?.let {
            Text(
                text = it,
                color = if (it.startsWith("Unable")) {
                    Color.Red
                } else {
                    LightPurple
                },
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))
        }

        results.forEach { item ->
            SongSearchItem(
                song = item,
                isLoading = loadingVideoId == item.videoId,
                onPlay = {
                    if (!item.isArtist && loadingVideoId == null) {
                        scope.launch {
                            loadingVideoId = item.videoId
                            playbackMessage = "Resolving audio stream..."

                            try {
                                val stream =
                                    streamExtraction.resolve(item)

                                musicPlayer.play(
                                    streamUrl = stream.url,
                                    title = item.title,
                                    artist = item.artist,
                                    artworkUrl = item.artworkUrl,
                                    requestHeaders = stream.requestHeaders
                                )

                                playingTitle = item.title
                                playbackMessage = "Playback started"
                            } catch (e: Exception) {
                                playbackMessage =
                                    "Unable to play: ${e.message ?: "Unknown error"}"
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
                        if (song.isArtist) {
                            CircleShape
                        } else {
                            RoundedCornerShape(12.dp)
                        }
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(
                        if (song.isArtist) {
                            CircleShape
                        } else {
                            RoundedCornerShape(12.dp)
                        }
                    )
                    .background(CardColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (song.isArtist) {
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
                fontWeight = FontWeight.Bold
            )

            Text(
                if (song.isArtist) {
                    "ARTIST"
                } else {
                    song.artist
                },
                color = LightPurple,
                fontSize = 12.sp
            )
        }

        if (song.isArtist) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Artist",
                tint = LightPurple,
                modifier = Modifier.size(20.dp)
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
                        contentDescription = "Play ${song.title}",
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
            title,
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        Text(
            "Your Music Nova experience starts here.",
            color = Color.LightGray
        )
    }
}

@Composable
fun FloatingNavigation(
    selectedTab: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        "Home" to Icons.Default.Home,
        "Search" to Icons.Default.Search,
        "Library" to Icons.Default.LibraryMusic,
        "Profile" to Icons.Default.Person
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color(0xFF211A30).copy(alpha = 0.97f),
                CircleShape
            )
            .padding(
                vertical = 12.dp,
                horizontal = 8.dp
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (name, icon) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        onSelect(name)
                    }
                    .padding(horizontal = 8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = if (selectedTab == name) {
                        LightPurple
                    } else {
                        Color.Gray
                    },
                    modifier = Modifier.size(23.dp)
                )

                Text(
                    name,
                    color = if (selectedTab == name) {
                        LightPurple
                    } else {
                        Color.Gray
                    },
                    fontSize = 10.sp
                )
            }
        }
    }
}


