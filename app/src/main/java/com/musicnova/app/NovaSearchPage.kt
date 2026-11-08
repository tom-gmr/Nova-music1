package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import com.musicnova.app.data.music.*
import kotlinx.coroutines.launch

private val SearchBg = Color(0xFF09080E)
private val SearchCard = Color(0xFF1B1722)
private val SearchAccent = Color(0xFFC9B9E5)
private val SearchMuted = Color(0xFFAAA4B5)

@Composable
fun NovaSearchScreen(
    libraryStore: LibraryStore,
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    val api = remember { InnerTubeMusicApi() }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
        if (query.isBlank() || loading) return

        scope.launch {
            loading = true
            searched = true
            errorMessage = null
            results = emptyList()

            try {
                results = api.searchSongs(query.trim(), 30)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Search failed. Try again."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBg)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Text(
            "DISCOVER MUSIC",
            color = SearchAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(5.dp))

        Text(
            "Find your sound.",
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Search songs and discover new music.",
            color = SearchMuted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Songs, artists, albums...", color = SearchMuted)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = SearchAccent
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = SearchMuted
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { searchMusic() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = SearchAccent,
                focusedBorderColor = SearchAccent,
                unfocusedBorderColor = Color(0xFF393241),
                focusedContainerColor = SearchCard,
                unfocusedContainerColor = SearchCard
            )
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = { searchMusic() },
            enabled = query.isNotBlank() && !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF49395B),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF28232E)
            )
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Search Music", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(18.dp))

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 45.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = SearchAccent
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Finding your music...",
                        color = SearchMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (errorMessage != null) {
            SearchMessage(
                title = "Something went wrong",
                subtitle = errorMessage ?: "Please try again."
            )
        } else if (results.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Search Results",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${results.size} found",
                    color = SearchMuted,
                    fontSize = 11.sp
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(results, key = {
                    if (it.isArtist) "artist_${it.title}"
                    else it.videoId
                }) { track ->
                    SearchSongCard(
                        track = track,
                        isLiked = !track.isArtist &&
                            libraryStore.isLiked(track.videoId),
                        onPlay = {
                            if (!track.isArtist) onPlay(track)
                        },
                        onLike = {
                            if (!track.isArtist) {
                                libraryStore.toggleLike(track.toSearchLibraryTrack())
                                refreshKey++
                            }
                        },
                        onPlaylist = {
                            selectedTrack = track
                            showPlaylistDialog = true
                        }
                    )
                }
            }
        } else if (searched) {
            SearchMessage(
                title = "No music found",
                subtitle = "Try another song or artist name."
            )
        } else {
            SearchMessage(
                title = "What do you want to hear?",
                subtitle = "Search your favourite songs and artists."
            )
        }
    }

    if (showPlaylistDialog && selectedTrack != null) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = {
                Text("Add to Playlist", color = Color.White)
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
                                    val added = libraryStore.addToPlaylist(
                                        playlist.name,
                                        selectedTrack!!.toSearchLibraryTrack()
                                    )
                                    showPlaylistDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    playlist.name,
                                    color = SearchAccent
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPlaylistDialog = false
                }) {
                    Text("Close", color = SearchAccent)
                }
            },
            containerColor = SearchCard
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
            .clip(RoundedCornerShape(17.dp))
            .background(SearchCard)
            .clickable(enabled = !track.isArtist) { onPlay() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = track.title,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF30283C)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(11.dp))

        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (track.isArtist) "Artist" else track.artist,
                color = SearchMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        if (!track.isArtist) {
            IconButton(onClick = onLike) {
                Icon(
                    if (isLiked) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) SearchAccent else SearchMuted
                )
            }

            IconButton(onClick = onPlaylist) {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = SearchMuted
                )
            }

            IconButton(onClick = onPlay) {
                Icon(
                    Icons.Default.PlayCircleFilled,
                    contentDescription = "Play",
                    tint = SearchAccent
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
            .clip(RoundedCornerShape(20.dp))
            .background(SearchCard)
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = null,
            tint = SearchAccent,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            subtitle,
            color = SearchMuted,
            fontSize = 11.sp
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
