
package com.musicnova.app

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
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
import org.json.JSONArray

private val SearchBg = Color(0xFF09080E)
private val SearchSurface = Color(0xFF17131F)
private val SearchCard = Color(0xFF211B2B)
private val SearchPurple = Color(0xFF9277B8)
private val SearchLavender = Color(0xFFD1C1EB)
private val SearchMuted = Color(0xFFAAA4B5)
private val SearchBorder = Color.White.copy(alpha = 0.09f)

private data class SearchCategory(
    val title: String,
    val subtitle: String,
    val colors: List<Color>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val searchCategories = listOf(
    SearchCategory("Bollywood", "Hindi hits", listOf(Color(0xFF75445F), Color(0xFF342039)), Icons.Default.Movie),
    SearchCategory("Pop", "Popular music", listOf(Color(0xFF66518B), Color(0xFF29203E)), Icons.Default.MusicNote),
    SearchCategory("Hip-Hop", "Beats & rap", listOf(Color(0xFF98613E), Color(0xFF39251F)), Icons.Default.GraphicEq),
    SearchCategory("Romance", "Love songs", listOf(Color(0xFF99516E), Color(0xFF3D2338)), Icons.Default.Favorite),
    SearchCategory("Electronic", "Electronic sounds", listOf(Color(0xFF456D79), Color(0xFF202C3A)), Icons.Default.Bolt),
    SearchCategory("Chill", "Relax & unwind", listOf(Color(0xFF526F70), Color(0xFF202D32)), Icons.Default.Headphones)
)

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
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("nova_search_history", Context.MODE_PRIVATE)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val results = uiState.searchResults
    val loading = uiState.isSearching
    val errorMessage = uiState.searchError

    var query by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<YouTubeMusicTrack?>(null) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    var recentSearches by remember {
        mutableStateOf(
            runCatching {
                val json = JSONArray(prefs.getString("recent_queries", "[]"))
                List(json.length()) { json.getString(it) }
            }.getOrDefault(emptyList())
        )
    }

    val playlists = remember(refreshKey) {
        libraryStore.getPlaylists()
    }

    fun saveSearch(term: String) {
        val updated = (listOf(term.trim()) + recentSearches)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(10)

        recentSearches = updated
        prefs.edit()
            .putString("recent_queries", JSONArray(updated).toString())
            .apply()
    }

    fun performSearch(term: String) {
        val cleanTerm = term.trim()
        if (cleanTerm.isBlank() || loading) return

        query = cleanTerm
        searched = true
        saveSearch(cleanTerm)
        viewModel.searchSongs(cleanTerm)
    }

    val orderedTracks = remember(results) {
        val tracks = results.filter { !it.isArtist && it.videoId.isNotBlank() }
        val artists = results.filter { it.isArtist }
        val topTrack = tracks.firstOrNull { it.isOfficial } ?: tracks.firstOrNull()

        listOfNotNull(topTrack) +
            tracks.filter { it.videoId != topTrack?.videoId } +
            artists
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SearchBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                "MUSIC NOVA",
                color = SearchLavender,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(7.dp))

            Text(
                "Search",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Find your next favourite sound.",
                color = SearchMuted,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SearchSurface)
                    .border(1.dp, SearchBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SearchLavender
                )

                Spacer(Modifier.width(10.dp))

                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        searched = false
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Songs, artists, albums...",
                            color = SearchMuted,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { performSearch(query) }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = SearchLavender,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        searched = false
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = SearchMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            when {
                loading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SearchLavender)
                            Spacer(Modifier.height(14.dp))
                            Text("Finding your music...", color = SearchMuted)
                        }
                    }
                }

                errorMessage != null && searched -> {
                    SearchMessage(
                        title = "Something went wrong",
                        subtitle = errorMessage
                    )
                }

                searched && results.isNotEmpty() -> {
                    Text(
                        "RESULTS FOR YOU",
                        color = SearchLavender,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 25.dp)
                    ) {
                        val topTrack = orderedTracks.firstOrNull { !it.isArtist }

                        if (topTrack != null) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Top Song",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        if (topTrack.isOfficial) "OFFICIAL" else "TOP MATCH",
                                        color = SearchLavender,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            item {
                                SearchSongCard(
                                    track = topTrack,
                                    isLiked = libraryStore.isLiked(topTrack.videoId),
                                    onPlay = {
                                        onPlayQueue(
                                            orderedTracks.filter { !it.isArtist },
                                            topTrack
                                        )
                                    },
                                    onLike = {
                                        libraryStore.toggleLike(topTrack.toSearchLibraryTrack())
                                        refreshKey++
                                    },
                                    onPlaylist = {
                                        selectedTrack = topTrack
                                        showPlaylistDialog = true
                                    },
                                    isTopSong = true
                                )
                            }
                        }

                        val remaining = orderedTracks.filter {
                            it.videoId != topTrack?.videoId
                        }

                        if (remaining.isNotEmpty()) {
                            item {
                                Text(
                                    "More Results",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        items(
                            remaining,
                            key = {
                                if (it.isArtist) "artist_${it.title}" else it.videoId
                            }
                        ) { track ->
                            SearchSongCard(
                                track = track,
                                isLiked = !track.isArtist &&
                                    libraryStore.isLiked(track.videoId),
                                onPlay = {
                                    if (!track.isArtist) {
                                        onPlayQueue(
                                            orderedTracks.filter { !it.isArtist },
                                            track
                                        )
                                    }
                                },
                                onLike = {
                                    if (!track.isArtist) {
                                        libraryStore.toggleLike(track.toSearchLibraryTrack())
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
                }

                searched -> {
                    SearchMessage(
                        title = "No music found",
                        subtitle = "Try another song or artist name."
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 25.dp)
                    ) {
                        if (recentSearches.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Recent Searches",
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.weight(1f))

                                    TextButton(onClick = {
                                        recentSearches = emptyList()
                                        prefs.edit().remove("recent_queries").apply()
                                    }) {
                                        Text("Clear", color = SearchLavender)
                                    }
                                }
                            }

                            items(recentSearches, key = { it.lowercase() }) { term ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { performSearch(term) }
                                        .padding(vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = SearchMuted
                                    )

                                    Spacer(Modifier.width(13.dp))

                                    Text(
                                        term,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    IconButton(
                                        onClick = {
                                            val updated = recentSearches.filterNot {
                                                it.equals(term, ignoreCase = true)
                                            }
                                            recentSearches = updated
                                            prefs.edit()
                                                .putString(
                                                    "recent_queries",
                                                    JSONArray(updated).toString()
                                                )
                                                .apply()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove search",
                                            tint = SearchMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(18.dp)) }
                        }

                        item {
                            Text(
                                "Browse Categories",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(14.dp))
                        }

                        items(searchCategories.chunked(2)) { rowCategories ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowCategories.forEach { category ->
                                    SearchCategoryCard(
                                        category = category,
                                        modifier = Modifier.weight(1f),
                                        onClick = { performSearch(category.title) }
                                    )
                                          )
                                }

                                if (rowCategories.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlaylistDialog && selectedTrack != null) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
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
                                Text(playlist.name, color = SearchLavender)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Close", color = SearchLavender)
                }
            },
            containerColor = SearchSurface
        )
    }
}

@Composable
private fun SearchCategoryCard(
    category: SearchCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(category.colors))
            .border(1.dp, SearchBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            category.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(25.dp)
        )

        Column {
            Text(
                category.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                category.subtitle,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SearchSongCard(
    track: YouTubeMusicTrack,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onPlaylist: () -> Unit,
    isTopSong: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isTopSong) 22.dp else 17.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (isTopSong) Color(0xFF30233F) else SearchCard,
                        Color(0xFF17141F)
                    )
                )
            )
            .border(1.dp, SearchBorder, RoundedCornerShape(20.dp))
            .clickable(enabled = !track.isArtist, onClick = onPlay)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = track.title,
            modifier = Modifier
                .size(if (isTopSong) 68.dp else 56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SearchSurface),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            Text(
                if (track.isArtist) "Artist" else track.artist,
                color = SearchMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!track.isArtist) {
            IconButton(onClick = onLike, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) SearchLavender else SearchMuted,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(onClick = onPlaylist, modifier = Modifier.size(36.dp)) {
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
            .padding(top = 25.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SearchSurface)
            .border(1.dp, SearchBorder, RoundedCornerShape(24.dp))
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.GraphicEq,
            contentDescription = null,
            tint = SearchLavender,
            modifier = Modifier.size(38.dp)
        )

        Spacer(Modifier.height(15.dp))

        Text(
            title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(7.dp))

        Text(
            subtitle,
            color = SearchMuted,
            fontSize = 12.sp
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
