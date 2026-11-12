package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.LibraryStore
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.MusicPlaylist

private val LibBackground = Color(0xFF09080D)
private val LibCard = Color(0xFF191620)
private val LibAccent = Color(0xFFC9B9E5)
private val LibMuted = Color(0xFFAAA4B5)
private val LibCardHighlight = Color(0xFF282231)

@Composable
fun LibraryScreen(
    libraryStore: LibraryStore,
    onPlay: (LibraryTrack) -> Unit
) {
    var section by remember { mutableStateOf("Playlists") }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("Newest") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val likedSongs = remember(refreshKey) { libraryStore.getLikedSongs() }
    val recentSongs = remember(refreshKey) { libraryStore.getRecentlyPlayed() }
    val playlists = remember(refreshKey) { libraryStore.getPlaylists() }
    val currentPlaylist = playlists.firstOrNull { it.name == selectedPlaylist }

    val filteredPlaylists = remember(playlists, searchQuery, sortMode) {
        val filtered = playlists.filter { playlist ->
            searchQuery.isBlank() ||
                playlist.name.contains(searchQuery, ignoreCase = true) ||
                playlist.tracks.any { track ->
                    track.title.contains(searchQuery, ignoreCase = true) ||
                        track.artist.contains(searchQuery, ignoreCase = true)
                }
        }
        when (sortMode) {
            "Name A-Z" -> filtered.sortedBy { it.name.lowercase() }
            "Name Z-A" -> filtered.sortedByDescending { it.name.lowercase() }
            "Most songs" -> filtered.sortedByDescending { it.tracks.size }
            else -> filtered
        }
    }

    val filteredLikedSongs = remember(likedSongs, searchQuery) {
        likedSongs.filter {
            searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredRecentSongs = remember(recentSongs, searchQuery) {
        recentSongs.filter {
            searchQuery.isBlank() ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    if (selectedPlaylist != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LibBackground)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedPlaylist = null; searchQuery = "" }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LibAccent)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        selectedPlaylist.orEmpty(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${currentPlaylist?.tracks?.size ?: 0} songs",
                        color = LibMuted,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = {
                    selectedPlaylist?.let { libraryStore.deletePlaylist(it) }
                    selectedPlaylist = null
                    refreshKey++
                }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete playlist", tint = LibMuted)
                }
            }

            Spacer(Modifier.height(12.dp))
            val tracks = currentPlaylist?.tracks.orEmpty()
            if (tracks.isEmpty()) {
                LibraryEmptyState(
                    title = "This playlist is empty",
                    subtitle = "Add songs from Search to build your collection."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(tracks, key = { it.videoId }) { track ->
                        LibrarySongRow(
                            track = track,
                            onPlay = { onPlay(track) },
                            trailing = {
                                IconButton(onClick = {
                                    selectedPlaylist?.let { libraryStore.removeFromPlaylist(it, track.videoId) }
                                    refreshKey++
                                }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = LibMuted)
                                }
                            }
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LibBackground)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    val heading = when (section) {
                        "Liked Songs" -> "Liked Songs"
                        "Recent" -> "Recently Played"
                        else -> "Playlists"
                    }
                    val subtitle = when (section) {
                        "Liked Songs" -> "${likedSongs.size} songs saved"
                        "Recent" -> "${recentSongs.size} recently played songs"
                        else -> "${playlists.size} playlists • ${playlists.sumOf { it.tracks.size }} songs"
                    }
                    Text(
                        heading,
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, color = LibMuted, fontSize = 12.sp)
                }

                if (section == "Playlists") {
                    IconButton(onClick = {
                        playlistName = ""
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Create playlist", tint = LibAccent)
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort playlists", tint = LibAccent)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = LibCardHighlight
                        ) {
                            listOf("Newest", "Name A-Z", "Name Z-A", "Most songs").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
                                    onClick = {
                                        sortMode = option
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Playlists", "Liked Songs", "Recent").forEach { tab ->
                    LibraryTab(
                        title = tab,
                        selected = section == tab,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            section = tab
                            searchQuery = ""
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        when (section) {
                            "Liked Songs" -> "Search liked songs..."
                            "Recent" -> "Search listening history..."
                            else -> "Search playlists..."
                        },
                        color = LibMuted,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = LibMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = LibMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = LibCard,
                    unfocusedContainerColor = LibCard,
                    focusedBorderColor = LibAccent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = LibAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

            Spacer(Modifier.height(12.dp))

            when (section) {
                "Liked Songs" -> {
                    if (filteredLikedSongs.isEmpty()) {
                        LibraryEmptyState(
                            title = if (likedSongs.isEmpty()) "No liked songs yet" else "No matching songs",
                            subtitle = if (likedSongs.isEmpty()) "Tap the heart on a song in Search to save it here." else "Try another search."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 28.dp)
                        ) {
                            items(filteredLikedSongs, key = { it.videoId }) { track ->
                                LibrarySongRow(
                                    track = track,
                                    onPlay = { onPlay(track) },
                                    trailing = {
                                        IconButton(onClick = {
                                            libraryStore.removeLikedSong(track.videoId)
                                            refreshKey++
                                        }) {
                                            Icon(Icons.Default.Favorite, contentDescription = "Unlike", tint = LibAccent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                "Recent" -> {
                    if (filteredRecentSongs.isEmpty()) {
                        LibraryEmptyState(
                            title = if (recentSongs.isEmpty()) "Nothing played yet" else "No matching songs",
                            subtitle = if (recentSongs.isEmpty()) "Your listening history will appear here." else "Try another search."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 28.dp)
                        ) {
                            items(filteredRecentSongs, key = { it.videoId }) { track ->
                                LibrarySongRow(track = track, onPlay = { onPlay(track) })
                            }
                        }
                    }
                }

                else -> {
                    if (filteredPlaylists.isEmpty()) {
                        LibraryEmptyState(
                            title = if (playlists.isEmpty()) "No playlists yet" else "No matching playlists",
                            subtitle = if (playlists.isEmpty()) "Create your first playlist with the + button." else "Try another search."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 28.dp)
                        ) {
                            items(filteredPlaylists, key = { it.name }) { playlist ->
                                LibraryPlaylistCard(
                                    playlist = playlist,
                                    onOpen = { selectedPlaylist = playlist.name },
                                    onPlay = { playlist.tracks.firstOrNull()?.let(onPlay) },
                                    onDelete = {
                                        libraryStore.deletePlaylist(playlist.name)
                                        refreshKey++
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = LibAccent,
                        unfocusedBorderColor = LibMuted,
                        focusedLabelColor = LibAccent,
                        unfocusedLabelColor = LibMuted,
                        cursorColor = LibAccent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (libraryStore.createPlaylist(playlistName)) {
                            refreshKey++
                            searchQuery = ""
                            section = "Playlists"
                            showCreateDialog = false
                        }
                    },
                    enabled = playlistName.isNotBlank()
                ) { Text("Create", color = LibAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = LibMuted)
                }
            },
            containerColor = LibCard,
            titleContentColor = Color.White,
            textContentColor = LibMuted
        )
    }
}

@Composable
private fun LibraryPlaylistCard(
    playlist: MusicPlaylist,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember(playlist.name) { mutableStateOf(false) }
    val cover = playlist.tracks.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LibCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LibCardHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = LibAccent,
                    modifier = Modifier.size(25.dp)
                )
                if (!cover.isNullOrBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = "${playlist.name} cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${playlist.tracks.size} songs",
                    color = LibMuted,
                    fontSize = 12.sp,
                    maxLine = 1
                )
            }

            if (playlist.tracks.isNotEmpty()) {
                Surface(
                    onClick = onPlay,
                    shape = CircleShape,
                    color = Color(0xFF352B42),
                    contentColor = LibAccent,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play playlist", modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(3.dp))
            }

            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Playlist options", tint = LibMuted)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = LibCardHighlight
                ) {
                    DropdownMenuItem(
                        text = { Text("Open playlist", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = LibMuted) },
                        onClick = { menuExpanded = false; onOpen() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFFF8A8A)) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF8A8A)) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTab(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFF3A3048) else LibCard,
        contentColor = if (selected) LibAccent else LibMuted
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LibrarySongRow(
    track: LibraryTrack,
    onPlay: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(LibCard)
            .clickable(onClick = onPlay)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(49.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LibCardHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = LibAccent)
            if (!track.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                track.artist,
                color = LibMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = LibAccent)
        }
        trailing?.invoke()
    }
}

@Composable
private fun LibraryEmptyState(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LibCard)
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = LibAccent, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = LibMuted, fontSize = 11.sp)
    }
}
