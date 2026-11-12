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
import coil.compose.AsyncImage
import com.musicnova.app.data.music.LibraryStore
import com.musicnova.app.data.music.LibraryTrack

private val LibBackground = Color(0xFF09080D)
private val LibCard = Color(0xFF191620)
private val LibAccent = Color(0xFFC9B9E5)
private val LibMuted = Color(0xFFAAA4B5)

@Composable
fun LibraryScreen(
    libraryStore: LibraryStore,
    onPlay: (LibraryTrack) -> Unit
) {
    var section by remember { mutableStateOf("Liked Songs") }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }

    val likedSongs = remember(refreshKey) {
        libraryStore.getLikedSongs()
    }
    val recentSongs = remember(refreshKey) {
        libraryStore.getRecentlyPlayed()
    }
    val playlists = remember(refreshKey) {
        libraryStore.getPlaylists()
    }
    val currentPlaylist = playlists.firstOrNull {
        it.name == selectedPlaylist
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LibBackground)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        if (selectedPlaylist != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedPlaylist = null }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = LibAccent
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        selectedPlaylist.orEmpty(),
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
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
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete playlist",
                        tint = LibMuted
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val tracks = currentPlaylist?.tracks.orEmpty()

            if (tracks.isEmpty()) {
                LibraryEmptyState(
                    "This playlist is empty",
                    "Add songs from Search to build your collection."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(tracks, key = { it.videoId }) { track ->
                        LibrarySongRow(
                            track = track,
                            onPlay = { onPlay(track) },
                            trailing = {
                                IconButton(onClick = {
                                    selectedPlaylist?.let {
                                        libraryStore.removeFromPlaylist(
                                            it,
                                            track.videoId
                                        )
                                    }
                                    refreshKey++
                                }) {
                                    Icon(
                                        Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Remove",
                                        tint = LibMuted
                                    )
                                }
                            }
                        )
                    }
                }
            }
        } else {
            Text(
                "YOUR SPACE",
                color = LibAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(5.dp))

            Text(
                "Your Library",
                color = Color.White,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your music, your collection.",
                color = LibMuted,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(22.dp))

            LibraryHeroCard(
                likedCount = likedSongs.size,
                recentCount = recentSongs.size
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Liked Songs", "Recent", "Playlists").forEach {
                    LibraryTab(
                        title = it,
                        selected = section == it,
                        modifier = Modifier.weight(1f),
                        onClick = { section = it }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            when (section) {
                "Liked Songs" -> {
                    LibraryHeading(
                        "Liked Songs",
                        likedSongs.size.toString()
                    )

                    if (likedSongs.isEmpty()) {
                        LibraryEmptyState(
                            "No liked songs yet",
                            "Tap the heart on a song in Search to save it here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(likedSongs, key = { it.videoId }) { track ->
                                LibrarySongRow(
                                    track = track,
                                    onPlay = { onPlay(track) },
                                    trailing = {
                                        IconButton(onClick = {
                                            libraryStore.removeLikedSong(
                                                track.videoId
                                            )
                                            refreshKey++
                                        }) {
                                            Icon(
                                                Icons.Default.Favorite,
                                                contentDescription = "Unlike",
                                                tint = LibAccent
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                "Recent" -> {
                    LibraryHeading(
                        "Recently Played",
                        recentSongs.size.toString()
                    )

                    if (recentSongs.isEmpty()) {
                        LibraryEmptyState(
                            "Nothing played yet",
                            "Your listening history will appear here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(recentSongs, key = { it.videoId }) { track ->
                                LibrarySongRow(
                                    track = track,
                                    onPlay = { onPlay(track) }
                                )
                            }
                        }
                    }
                }
                                "Playlists" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Your Playlists",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${playlists.size} playlists",
                                    color = LibMuted,
                                    fontSize = 12.sp
                                )
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    playlistName = ""
                                    showCreateDialog = true
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF352B42),
                                    contentColor = LibAccent
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Create playlist"
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(
                                            Color(0xFF40334F),
                                            Color(0xFF211B2B)
                                        )
                                    )
                                )
                                .clickable {
                                    playlistName = ""
                                    showCreateDialog = true
                                }
                                .padding(17.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(Color(0xFF57446D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = LibAccent,
                                    modifier = Modifier.size(29.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Create new playlist",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Collect songs your way",
                                    color = LibMuted,
                                    fontSize = 12.sp
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = LibAccent
                            )
                        }

                        Spacer(Modifier.height(22.dp))

                        if (playlists.isEmpty()) {
                            LibraryEmptyState(
                                "No playlists yet",
                                "Create your first playlist."
                            )
                        } else {
                            Text(
                                "All Playlists",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(13.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(15.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(playlists, key = { it.name }) { playlist ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color(0xFF30283C))
                                            .clickable {
                                                selectedPlaylist = playlist.name
                                            }
                                    ) {
                                        val cover = playlist.tracks
                                            .firstOrNull {
                                                !it.artworkUrl.isNullOrBlank()
                                            }
                                            ?.artworkUrl

                                        if (!cover.isNullOrBlank()) {
                                            AsyncImage(
                                                model = cover,
                                                contentDescription = playlist.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.QueueMusic,
                                                    contentDescription = null,
                                                    tint = LibAccent,
                                                    modifier = Modifier.size(55.dp)
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            Color(0x6609080D),
                                                            Color(0xFF09080D)
                                                        )
                                                    )
                                                )
                                        )

                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 17.dp,
                                                    end = 12.dp,
                                                    bottom = 15.dp
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    playlist.name,
                                                    color = Color.White,
                                                    fontSize = 19.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )

                                                Spacer(Modifier.height(5.dp))

                                                Text(
                                                    "${playlist.tracks.size} songs",
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 12.sp
                                                )
                                            }

                                            if (playlist.tracks.isNotEmpty()) {
                                                IconButton(
                                                    onClick = {
                                                        onPlay(playlist.tracks.first())
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.PlayCircleFilled,
                                                        contentDescription = "Play playlist",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(35.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    selectedPlaylist = playlist.name
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = "Open playlist",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(27.dp)
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
        if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "Create Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
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
                        unfocusedLabelColor = LibMuted
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (libraryStore.createPlaylist(playlistName)) {
                            refreshKey++
                            showCreateDialog = false
                        }
                    },
                    enabled = playlistName.isNotBlank()
                ) {
                    Text("Create", color = LibAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                }) {
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
private fun LibraryHeroCard(
    likedCount: Int,
    recentCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        Color(0xFF30263E),
                        Color(0xFF1C1925),
                        Color(0xFF17151D)
                    )
                )
            )
            .padding(19.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(Color(0xFF49395B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = LibAccent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.width(15.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "YOUR MUSIC COLLECTION",
                color = LibAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "$likedCount liked • $recentCount recent",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Made for your listening mood",
                color = LibMuted,
                fontSize = 11.sp
            )
        }

        Icon(
            Icons.Default.GraphicEq,
            contentDescription = null,
            tint = LibAccent,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun LibraryTab(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Color(0xFF3A3048) else LibCard
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            color = if (selected) LibAccent else LibMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun LibraryHeading(
    title: String,
    count: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(9.dp))
        Text(
            count,
            color = LibMuted,
            fontSize = 12.sp
        )
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
            .clickable { onPlay() }
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!track.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(49.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(49.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF30283C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = LibAccent
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
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                track.artist,
                color = LibMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        Icon(
            Icons.Default.PlayCircleFilled,
            contentDescription = "Play",
            tint = LibAccent,
            modifier = Modifier
                .size(27.dp)
                .clickable { onPlay() }
        )

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
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = LibAccent,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Text(
            subtitle,
            color = LibMuted,
            fontSize = 11.sp
        )
    }
}
