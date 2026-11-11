package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.musicnova.app.data.music.*
import com.musicnova.app.ui.home.MusicViewModel

private val SearchBg = Color(0xFF09080E)
private val SearchSurface = Color(0xFF17131F)
private val SearchCard = Color(0xFF211B2B)
private val SearchPurple = Color(0xFF9277B8)
private val SearchLavender = Color(0xFFD1C1EB)
private val SearchMuted = Color(0xFFAAA4B5)
private val SearchBorder = Color.White.copy(alpha = 0.09f)

@Composable
fun NovaSearchScreen(
    libraryStore: LibraryStore,
    onPlay: (YouTubeMusicTrack) -> Unit,
    onPlayQueue: (
        tracks: List<YouTubeMusicTrack>,
        selectedTrack: YouTubeMusicTrack
    ) -> Unit,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val results = uiState.searchResults
    val loading = uiState.isSearching
    val errorMessage = uiState.searchError

    var query by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf(false) }

    var selectedTrack by remember {
        mutableStateOf<YouTubeMusicTrack?>(null)
    }

    var showPlaylistDialog by remember {
        mutableStateOf(false)
    }

    var refreshKey by remember { mutableIntStateOf(0) }

    val playlists = remember(refreshKey) {
        libraryStore.getPlaylists()
    }

    fun searchMusic() {
        // Shared Search header
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 14.dp, bottom = 16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        "Search",
        color = Color.White,
        fontSize = 27.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.weight(1f))
    Icon(
        Icons.Default.GraphicEq,
        contentDescription = "Music Nova",
        tint = SearchLavender,
        modifier = Modifier.size(25.dp)
    )
}

Row(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(
            Brush.linearGradient(
                listOf(Color(0xFF292135), Color(0xFF191520))
            )
        )
        .border(1.dp, SearchBorder, RoundedCornerShape(24.dp))
        .padding(start = 15.dp, end = 7.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        Icons.Default.Search,
        contentDescription = null,
        tint = SearchLavender
    )

    Spacer(Modifier.width(10.dp))

    TextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.weight(1f),
        placeholder = {
            Text("Songs, artists, albums...", color = SearchMuted)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { searchMusic() }
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = SearchLavender,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )

    if (query.isNotEmpty()) {
        IconButton(onClick = { query = "" }) {
            Icon(Icons.Default.Close, "Clear", tint = SearchMuted)
        }
    } else {
        IconButton(
            onClick = { searchMusic() },
            enabled = !loading
        ) {
            Icon(Icons.Default.Search, "Search", tint = SearchLavender)
        }
    }
}

Spacer(Modifier.height(17.dp))

var selectedCategory by remember { mutableStateOf("Tracks") }

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(7.dp)
) {
    listOf("Tracks", "Podcasts", "Artists", "Albums").forEach { category ->
        val selected = selectedCategory == category

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selected) Color(0xFF49395D)
                    else Color(0xFF17131F)
                )
                .border(
                    1.dp,
                    if (selected) SearchPurple else SearchBorder,
                    RoundedCornerShape(18.dp)
                )
                .clickable { selectedCategory = category }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                category,
                color = if (selected) Color.White else SearchMuted,
                fontSize = 11.sp,
                fontWeight = if (selected)
                    FontWeight.SemiBold
                else
                    FontWeight.Medium
            )
        }
    }
}

Spacer(Modifier.height(19.dp))

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = SearchLavender,
                            strokeWidth = 3.dp
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Finding your music...",
                            color = SearchMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    SearchMessage(
                        title = "Something went wrong",
                        subtitle = errorMessage
                    )
                }
            } else if (results.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Search results",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "${results.size} tracks and artists",
                            color = SearchMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF30263D))
                            .padding(
                                horizontal = 13.dp,
                                vertical = 7.dp
                            )
                    ) {
                        Text(
                            "ONLINE",
                            color = SearchLavender,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 25.dp)
                ) {
                    items(
                        results,
                        key = {
                            if (it.isArtist) {
                                "artist_${it.title}"
                            } else {
                                it.videoId
                            }
                        }
                    ) { track ->

                        SearchSongCard(
                            track = track,
                            isLiked = !track.isArtist &&
                                libraryStore.isLiked(track.videoId),
                            onPlay = {
                                if (!track.isArtist) {
                                    val queueTracks = results.filter {
                                        !it.isArtist &&
                                            it.videoId.isNotBlank()
                                    }

                                    onPlayQueue(queueTracks, track)
                                }
                            },
                            onLike = {
                                if (!track.isArtist) {
                                    libraryStore.toggleLike(
                                        track.toSearchLibraryTrack()
                                    )
                                    refreshKey++
                                }
                            },
                            onPlaylist = {
                                if (!track.isArtist) {
                                    selectedTrack = track
                                    showPlaylistDialog = true
                                }
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    SearchMessage(
                        title = if (searched)
                            "No music found"
                        else
                            "Your next favourite starts here",
                        subtitle = if (searched)
                            "Try another song or artist name."
                        else
                            "Search millions of songs and discover something new."
                    )
                }
            }
        }
    }

    if (showPlaylistDialog && selectedTrack != null) {
        AlertDialog(
            onDismissRequest = {
                showPlaylistDialog = false
            },
            title = {
                Text(
                    "Add to Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (playlists.isEmpty()) {
                    Text(
                        "Create a playlist first from your Library.",
                        color = SearchMuted
                    )
                } else {
                    Column {
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    libraryStore.addToPlaylist(
                                        playlist.name,
                                        selectedTrack!!.toSearchLibraryTrack()
                                    )

                                    refreshKey++
                                    showPlaylistDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    playlist.name,
                                    color = SearchLavender
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPlaylistDialog = false
                    }
                ) {
                    Text(
                        "Close",
                        color = SearchLavender
                    )
                }
            },
            containerColor = SearchSurface
        )
    }
}

@Composable
private fun SearchSongCard(
    track: YouTubeMusicTrack,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF211B2B),
                        Color(0xFF17141F)
                    )
                )
            )
            .border(
                1.dp,
                SearchBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = !track.isArtist) {
                onPlay()
            }
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(59.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF30283C)),
                contentScale = ContentScale.Crop
            )

            if (!track.isArtist) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(SearchPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = if (track.isArtist) "Artist" else track.artist,
                color = SearchMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!track.isArtist) {
            IconButton(
                onClick = onLike,
                modifier = Modifier.size(37.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = "Like",
                    tint = if (isLiked) SearchLavender else SearchMuted,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onPlaylist,
                modifier = Modifier.size(37.dp)
            ) {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = SearchMuted,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchMessage(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF241D30),
                        Color(0xFF17141F)
                    )
                )
            )
            .border(
                1.dp,
                SearchBorder,
                RoundedCornerShape(25.dp)
            )
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(65.dp)
                .clip(CircleShape)
                .background(Color(0xFF392D49)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                tint = SearchLavender,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            subtitle,
            color = SearchMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

private fun YouTubeMusicTrack.toSearchLibraryTrack(): LibraryTrack {
    return LibraryTrack(
        videoId = videoId,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl
    )
}
