package com.musicnova.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.LibraryStore
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.MusicPlaylist

private enum class LibraryFilter {
    ALL,
    PLAYLISTS,
    LIKED,
    RECENT
}

@Composable
fun LibraryScreen(
    libraryStore: LibraryStore,
    onPlay: (LibraryTrack) -> Unit,
    onPlayQueue: (List<LibraryTrack>, LibraryTrack) -> Unit
) {
    var selectedFilter by remember {
        mutableStateOf(LibraryFilter.ALL)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var refresh by remember {
        mutableIntStateOf(0)
    }

    var expandedPlaylist by remember {
        mutableStateOf<String?>(null)
    }

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    var playlistName by remember {
        mutableStateOf("")
    }

    val likedSongs = remember(refresh) {
        libraryStore.getLikedSongs()
    }

    val recentSongs = remember(refresh) {
        libraryStore.getRecentlyPlayed()
    }

    val playlists = remember(refresh) {
        libraryStore.getPlaylists()
    }

    val backgroundColor = Color(0xFF09090D)
    val cardColor = Color(0xFF17171F)
    val accentColor = Color(0xFFB69CFF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 18.dp)
    ) {

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Your Library",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Your music, your collection.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(
                onClick = {
                    showCreateDialog = true
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create playlist",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search your library...")
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.DarkGray,
                focusedContainerColor = cardColor,
                unfocusedContainerColor = cardColor,
                cursorColor = accentColor
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = {
                        selectedFilter = filter
                    },
                    label = {
                        Text(
                            when (filter) {
                                LibraryFilter.ALL -> "All"
                                LibraryFilter.PLAYLISTS -> "Playlists"
                                LibraryFilter.LIKED -> "Liked"
                                LibraryFilter.RECENT -> "Recent"
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.Black,
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (
                selectedFilter == LibraryFilter.ALL ||
                selectedFilter == LibraryFilter.PLAYLISTS
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Playlists",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${playlists.size} playlists",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (playlists.isEmpty()) {
                    item {
                        EmptyLibraryCard(
                            title = "No playlists yet",
                            subtitle = "Create your first playlist."
                        )
                    }
                }

                items(
                    playlists.filter {
                        it.name.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                    },
                    key = { it.name }
                ) { playlist ->

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardColor)
                            .clickable {
                                expandedPlaylist =
                                    if (expandedPlaylist == playlist.name) {
                                        null
                                    } else {
                                        playlist.name
                                    }
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF302744)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    playlist.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    "${playlist.tracks.size} songs",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (playlist.tracks.isNotEmpty()) {
                                        onPlayQueue(
                                            playlist.tracks,
                                            playlist.tracks.first()
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play playlist",
                                    tint = accentColor
                                )
                            }
                        }

                        if (expandedPlaylist == playlist.name) {
                            Spacer(Modifier.height(12.dp))

                            playlist.tracks.forEach { track ->
                                TrackRow(
                                    track = track,
                                    onClick = {
                                        onPlay(track)
                                    },
                                    onRemove = {
                                        libraryStore.removeFromPlaylist(
                                            playlist.name,
                                            track.videoId
                                        )
                                        refresh++
                                    }
                                )
                            }

                            if (playlist.tracks.isEmpty()) {
                                Text(
                                    "This playlist is empty.",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            TextButton(
                                onClick = {
                                    libraryStore.deletePlaylist(
                                        playlist.name
                                    )
                                    expandedPlaylist = null
                                    refresh++
                                }
                            ) {
                                Text(
                                    "Delete playlist",
                                    color = Color(0xFFFF7777)
                                )
                            }
                        }
                    }
                }
            }

            if (
                selectedFilter == LibraryFilter.ALL ||
                selectedFilter == LibraryFilter.LIKED
            ) {
                item {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF6B8A)
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "Liked Songs",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val filteredLiked = likedSongs.filter {
                    it.title.contains(searchQuery, true) ||
                        it.artist.contains(searchQuery, true)
                }

                if (filteredLiked.isEmpty()) {
                    item {
                        EmptyLibraryCard(
                            title = "No liked songs",
                            subtitle = "Songs you like will appear here."
                        )
                    }
                }

                items(
                    filteredLiked,
                    key = { it.videoId }
                ) { track ->
                    TrackRow(
                        track = track,
                        onClick = {
                            onPlay(track)
                        },
                        onRemove = {
                            libraryStore.removeLikedSong(track.videoId)
                            refresh++
                        }
                    )
                }
            }
            if (
                selectedFilter == LibraryFilter.ALL ||
                selectedFilter == LibraryFilter.RECENT
            ) {
                item {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = accentColor
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "Recently Played",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val filteredRecent = recentSongs.filter {
                    it.title.contains(searchQuery, true) ||
                        it.artist.contains(searchQuery, true)
                }

                if (filteredRecent.isEmpty()) {
                    item {
                        EmptyLibraryCard(
                            title = "Nothing played yet",
                            subtitle = "Your recently played songs will appear here."
                        )
                    }
                }

                items(
                    filteredRecent,
                    key = { it.videoId }
                ) { track ->
                    TrackRow(
                        track = track,
                        onClick = {
                            onPlay(track)
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                playlistName = ""
            },
            title = {
                Text("Create Playlist")
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = {
                        playlistName = it
                    },
                    label = {
                        Text("Playlist name")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (
                            libraryStore.createPlaylist(playlistName)
                        ) {
                            refresh++
                            showCreateDialog = false
                            playlistName = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        playlistName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
