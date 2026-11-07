
package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val LibraryCard = Color(0xFF1B1822)
private val LibraryAccent = Color(0xFFC9B9E5)
private val LibraryMuted = Color(0xFFAAA4B5)

@Composable
fun LibraryScreen(
    libraryStore: LibraryStore,
    onPlay: (LibraryTrack) -> Unit
) {
    var selectedSection by remember { mutableStateOf("Liked Songs") }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }

    val likedSongs = remember(refreshKey) {
        libraryStore.getLikedSongs()
    }

    val recentSongs = remember(refreshKey) {
        libraryStore.getRecentlyPlayed()
    }

    val playlists = remember(refreshKey) {
        libraryStore.getPlaylists()
    }

    val currentPlaylist = remember(refreshKey, selectedPlaylist) {
        playlists.firstOrNull { it.name == selectedPlaylist }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (selectedPlaylist != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedPlaylist = null }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = LibraryAccent
                    )
                }

                Column {
                    Text(
                        selectedPlaylist ?: "",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${currentPlaylist?.tracks?.size ?: 0} songs",
                        color = LibraryMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = {
                        selectedPlaylist?.let {
                            libraryStore.deletePlaylist(it)
                        }
                        selectedPlaylist = null
                        refreshKey++
                    }
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete playlist",
                        tint = LibraryMuted
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val tracks = currentPlaylist?.tracks.orEmpty()

            if (tracks.isEmpty()) {
                EmptyLibraryMessage(
                    title = "This playlist is empty",
                    subtitle = "Add songs from Search to build your playlist."
                )
            } else {
                tracks.forEach { track ->
                    LibraryTrackRow(
                        track = track,
                        onPlay = { onPlay(track) },
                        trailingAction = {
                            IconButton(
                                onClick = {
                                    selectedPlaylist?.let {
                                        libraryStore.removeFromPlaylist(
                                            it,
                                            track.videoId
                                        )
                                    }
                                    refreshKey++
                                }
                            ) {
                                Icon(
                                    Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove from playlist",
                                    tint = LibraryMuted
                                )
                            }
                        }
                    )
                }
            }
        } else {
            Text(
                "Your Library",
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your saved music, all in one place.",
                color = LibraryMuted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                LibrarySectionChip(
                    title = "Liked Songs",
                    selected = selectedSection == "Liked Songs",
                    onClick = { selectedSection = "Liked Songs" }
                )

                LibrarySectionChip(
                    title = "Recent",
                    selected = selectedSection == "Recent",
                    onClick = { selectedSection = "Recent" }
                )

                LibrarySectionChip(
                    title = "Playlists",
                    selected = selectedSection == "Playlists",
                    onClick = { selectedSection = "Playlists" }
                )
            }

            Spacer(Modifier.height(18.dp))

            when (selectedSection) {
                "Liked Songs" -> {
                    LibrarySectionHeading(
                        title = "Liked Songs",
                        count = likedSongs.size
                    )

                    if (likedSongs.isEmpty()) {
                        EmptyLibraryMessage(
                            title = "No liked songs yet",
                            subtitle = "Use the heart button on songs in Search to save them here."
                        )
                    } else {
                        likedSongs.forEach { track ->
                            LibraryTrackRow(
                                track = track,
                                onPlay = { onPlay(track) },
                                trailingAction = {
                                    IconButton(
                                        onClick = {
                                            libraryStore.removeLikedSong(
                                                track.videoId
                                            )
                                            refreshKey++
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = "Unlike song",
                                            tint = LibraryAccent
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                "Recent" -> {
                    LibrarySectionHeading(
                        title = "Recently Played",
                        count = recentSongs.size
                    )

                    if (recentSongs.isEmpty()) {
                        EmptyLibraryMessage(
                            title = "Nothing played yet",
                            subtitle = "Songs you play will appear here."
                        )
                    } else {
                        recentSongs.forEach { track ->
                            LibraryTrackRow(
                                track = track,
                                onPlay = { onPlay(track) }
                            )
                        }
                    }
                }

                "Playlists" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LibrarySectionHeading(
                            title = "Your Playlists",
                            count = playlists.size,
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                playlistName = ""
                                showCreateDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create playlist",
                                tint = LibraryAccent
                            )
                        }
                    }

                    if (playlists.isEmpty()) {
                        EmptyLibraryMessage(
                            title = "No playlists yet",
                            subtitle = "Create a playlist to collect your favourite music."
                        )
                    } else {
                        playlists.forEach { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(LibraryCard)
                                    .clickable {
                                        selectedPlaylist = playlist.name
                                    }
                                    .padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(Color(0xFF30283C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = LibraryAccent,
                                        modifier = Modifier.size(27.dp)
                                    )
                                }

                                Spacer(Modifier.width(13.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        playlist.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )

                                    Text(
                                        "${playlist.tracks.size} songs",
                                        color = LibraryMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Open playlist",
                                    tint = LibraryMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text("Create playlist")
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
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
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = LibraryCard,
            titleContentColor = Color.White,
            textContentColor = LibraryMuted
        )
    }
}

@Composable
private fun LibrarySectionChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) Color(0xFF3A3048) else LibraryCard
            )
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp)
    ) {
        Text(
            title,
            color = if (selected) LibraryAccent else LibraryMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun LibrarySectionHeading(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(9.dp))

        Text(
            count.toString(),
            color = LibraryMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun LibraryTrackRow(
    track: LibraryTrack,
    onPlay: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!track.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(11.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(LibraryCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = LibraryAccent
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onPlay() }
        ) {
            Text(
                track.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Text(
                track.artist,
                color = LibraryMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        IconButton(onClick = onPlay) {
            Icon(
                Icons.Default.PlayCircleFilled,
                contentDescription = "Play song",
                tint = LibraryAccent,
                modifier = Modifier.size(27.dp)
            )
        }

        trailingAction?.invoke()
    }
}

@Composable
private fun EmptyLibraryMessage(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LibraryCard)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = LibraryAccent,
            modifier = Modifier.size(34.dp)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(5.dp))

        Text(
            subtitle,
            color = LibraryMuted,
            fontSize = 11.sp
        )
    }
}
