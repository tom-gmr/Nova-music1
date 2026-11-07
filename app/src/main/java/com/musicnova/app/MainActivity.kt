
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

    val musicPlayer = remember(context) { MusicPlayer(context) }
    val libraryStore = remember(context) { LibraryStore(context) }
    val streamExtraction = remember { StreamExtraction() }

    val playbackState by musicPlayer.playbackState.collectAsState()
    var playbackError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(musicPlayer) {
        onDispose { musicPlayer.release() }
    }

    fun playTrack(track: YouTubeMusicTrack) {
        if (track.isArtist || track.videoId.isBlank()) return

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
                libraryStore.addRecentlyPlayed(track.toLibraryTrack())
            } catch (e: Exception) {
                playbackError = "Unable to play: ${e.message ?: "Try again"}"
            }
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

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    IconButton(onClick = { selectedTab = "Profile" }) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = LightPurple,
                            modifier = Modifier.size(29.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 86.dp)
                ) {
                    Column(Modifier.fillMaxSize()) {
                        playbackError?.let {
                            Text(
                                it,
                                color = Color(0xFFFF9B9B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        when (selectedTab) {
                            "Home" -> RecommendationsHomeScreen(
                                onPlay = { playTrack(it) }
                            )

                            "Search" -> SearchScreen(
                                musicPlayer = musicPlayer,
                                libraryStore = libraryStore
                            )

                            "Library" -> LibraryScreen(
                                libraryStore = libraryStore,
                                onPlay = { track ->
                                    playTrack(track.toMusicTrack())
                                }
                            )

                            "Profile" -> ProfileScreen()
                        }
                    }
                }
            }

            if (playbackState.hasMedia) {
                GlassMiniPlayer(
                    state = playbackState,
                    onPlayPause = { musicPlayer.togglePlayPause() },
                    onStop = { musicPlayer.stop() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                )
            } else {
                FlatNavigation(
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private fun YouTubeMusicTrack.toLibraryTrack() = LibraryTrack(
    videoId = videoId,
    title = title,
    artist = artist,
    artworkUrl = artworkUrl
)

private fun LibraryTrack.toMusicTrack() = YouTubeMusicTrack(
    videoId = videoId,
    title = title,
    artist = artist,
    artworkUrl = artworkUrl,
    isArtist = false
)

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
            .shadow(20.dp, RoundedCornerShape(23.dp))
            .clip(RoundedCornerShape(23.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xE52D2738),
                        Color(0xE51B1923),
                        Color(0xE52A2434)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.18f),
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

        Column(Modifier.weight(1f)) {
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

        IconButton(onClick = onPlayPause, modifier = Modifier.size(42.dp)) {
            Icon(
                if (state.isPlaying) Icons.Default.PauseCircleFilled
                else Icons.Default.PlayCircleFilled,
                contentDescription = "Play or pause",
                tint = Color.White,
                modifier = Modifier.size(31.dp)
            )
        }

        IconButton(onClick = onStop, modifier = Modifier.size(42.dp)) {
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
fun FlatNavigation(
    selectedTab: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Triple("Home", Icons.Default.Home, "Home"),
        Triple("Search", Icons.Default.Search, "Search"),
        Triple("Library", Icons.Default.LibraryMusic, "Library"),
        Triple("Profile", Icons.Default.Person, "Profile")
    )

    Row(
        modifier = modifier.background(Black),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (label, icon, tab) ->
            val selected = selectedTab == tab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) LightPurple else MutedText,
                    modifier = Modifier.size(23.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    color = if (selected) LightPurple else MutedText,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold
                    else FontWeight.Normal
                )
            }
        }
    }
}

{
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }
    var loadingVideoId by remember { mutableStateOf<String?>(null) }
    var libraryRefresh by remember { mutableIntStateOf(0) }
    var selectedTrack by remember { mutableStateOf<LibraryTrack?>(null) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val api = remember { InnerTubeMusicApi() }
    val streamExtraction = remember { StreamExtraction() }
    val scope = rememberCoroutineScope()

    val likedIds = remember(libraryRefresh) {
        libraryStore.getLikedSongs().map { it.videoId }.toSet()
    }
    val playlists = remember(libraryRefresh) {
        libraryStore.getPlaylists()
    }

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
            placeholder = { Text("Search songs, artists...") },
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
                isLiked = !item.isArtist && item.videoId in likedIds,
                onPlay = {
                    if (!item.isArtist && loadingVideoId == null) {
                        scope.launch {
                            loadingVideoId = item.videoId
                            error = null
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
                                    item.toLibraryTrack()
                                )
                            } catch (e: Exception) {
                                error = "Unable to play: ${e.message ?: "Try again"}"
                            } finally {
                                loadingVideoId = null
                            }
                        }
                    }
                },
                onLike = {
                    if (!item.isArtist) {
                        libraryStore.toggleLike(item.toLibraryTrack())
                        libraryRefresh++
                    }
                },
                onAddPlaylist = {
                    if (!item.isArtist) {
                        selectedTrack = item.toLibraryTrack()
                        showPlaylistDialog = true
                    }
                }
            )
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
    var loadingVideoId by remember { mutableStateOf<String?>(null) }
    var libraryRefresh by remember { mutableIntStateOf(0) }
    var selectedTrack by remember { mutableStateOf<LibraryTrack?>(null) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val api = remember { InnerTubeMusicApi() }
    val streamExtraction = remember { StreamExtraction() }
    val scope = rememberCoroutineScope()

    val likedIds = remember(libraryRefresh) {
        libraryStore.getLikedSongs().map { it.videoId }.toSet()
    }
    val playlists = remember(libraryRefresh) {
        libraryStore.getPlaylists()
    }

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
            placeholder = { Text("Search songs, artists...") },
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
                isLiked = !item.isArtist && item.videoId in likedIds,
                onPlay = {
                    if (!item.isArtist && loadingVideoId == null) {
                        scope.launch {
                            loadingVideoId = item.videoId
                            error = null
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
                                    item.toLibraryTrack()
                                )
                            } catch (e: Exception) {
                                error = "Unable to play: ${e.message ?: "Try again"}"
                            } finally {
                                loadingVideoId = null
                            }
                        }
                    }
                },
                onLike = {
                    if (!item.isArtist) {
                        libraryStore.toggleLike(item.toLibraryTrack())
                        libraryRefresh++
                    }
                },
                onAddPlaylist = {
                    if (!item.isArtist) {
                        selectedTrack = item.toLibraryTrack()
                        showPlaylistDialog = true
                    }
                }
            )
        }
    }
    
    if (showPlaylistDialog && selectedTrack != null) {
        AlertDialog(
            onDismissRequest = {
                showPlaylistDialog = false
            },
            title = { Text("Add to playlist") },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text(
                            "You don't have any playlists yet.",
                            color = MutedText
                        )
                    } else {
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    libraryStore.addToPlaylist(
                                        playlist.name,
                                        selectedTrack!!
                                    )
                                    libraryRefresh++
                                    showPlaylistDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    playlist.name,
                                    color = LightPurple
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("New playlist name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newPlaylistName.trim()
                        if (name.isNotBlank() &&
                            libraryStore.createPlaylist(name)
                        ) {
                            libraryStore.addToPlaylist(
                                name,
                                selectedTrack!!
                            )
                            newPlaylistName = ""
                            libraryRefresh++
                            showPlaylistDialog = false
                        }
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPlaylistDialog = false
                    newPlaylistName = ""
                }) {
                    Text("Cancel")
                }
            },
            containerColor = CardColor,
            titleContentColor = Color.White,
            textContentColor = MutedText
        )
    }
}

@Composable
fun SongSearchItem(
    song: YouTubeMusicTrack,
    isLoading: Boolean,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onAddPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!song.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(54.dp)
                    .clip(
                        if (song.isArtist) CircleShape
                        else RoundedCornerShape(12.dp)
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(
                        if (song.isArtist) CircleShape
                        else RoundedCornerShape(12.dp)
                    )
                    .background(CardColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (song.isArtist) Icons.Default.Person
                    else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = LightPurple
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                if (song.isArtist) "Artist" else song.artist,
                color = MutedText,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        if (!song.isArtist) {
            IconButton(onClick = onLike) {
                Icon(
                    if (isLiked) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = "Like song",
                    tint = if (isLiked) LightPurple else MutedText
                )
            }

            IconButton(onClick = onAddPlaylist) {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = LightPurple
                )
            }

            IconButton(
                onClick = onPlay,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = LightPurple,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        contentDescription = "Play song",
                        tint = LightPurple,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Your Profile",
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF30283C),
                            Color(0xFF1B1822)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = LightPurple,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Music Nova",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Your personal music space",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        ProfileOption(
            icon = Icons.Default.Settings,
            title = "Settings",
            subtitle = "App preferences"
        )

        ProfileOption(
            icon = Icons.Default.Info,
            title = "About Music Nova",
            subtitle = "Your music, your world"
        )
    }
}

@Composable
private fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(CardColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = LightPurple,
            modifier = Modifier.size(25.dp)
        )

        Spacer(Modifier.width(14.dp))

        Column {
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = MutedText,
                fontSize = 11.sp
            )
        }
    }
}

            
           
