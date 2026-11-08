
package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
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
import com.musicnova.app.data.music.LibraryStore
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.YouTubeMusicTrack
import kotlinx.coroutines.launch

private val HomeBackground = Color(0xFF0D0B12)
private val HomeCard = Color(0xFF1B1822)
private val HomeAccent = Color(0xFFB9A2E8)
private val HomeMuted = Color(0xFFAAA4B5)

@Composable
fun RecommendationsHomeScreen(
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    val context = LocalContext.current

    val libraryStore = remember(context) {
        LibraryStore(context)
    }

    val api = remember {
        InnerTubeMusicApi()
    }

    val scope = rememberCoroutineScope()

    var recommendations by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }

    var trending by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }

    var popularArtists by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }

    var recentlyPlayed by remember {
        mutableStateOf<List<LibraryTrack>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }
    
    fun loadRecommendations() {
        scope.launch {
            loading = true
            error = null

            try {
                recentlyPlayed = libraryStore.getRecentlyPlayed()

                val likedSongs = libraryStore.getLikedSongs()

                val seeds = (likedSongs + recentlyPlayed)
                    .distinctBy { it.videoId }
                    .take(5)

                val results = mutableListOf<YouTubeMusicTrack>()

                if (seeds.isNotEmpty()) {
                    seeds.forEach { seed ->
                        val query = "songs like ${seed.title} by ${seed.artist}"

                        val songs = api.searchSongs(query, 15)

                        results.addAll(
                            songs.filterNot { it.isArtist }
                        )
                    }
                } else {
                    val songs = api.searchSongs(
                        "popular trending songs India",
                        30
                    )

                    results.addAll(
                        songs.filterNot { it.isArtist }
                    )
                }
                
                val excludedIds = (likedSongs + recentlyPlayed)
                    .map { it.videoId }
                    .toSet()

                recommendations = results
                    .distinctBy { it.videoId }
                    .filterNot { it.videoId in excludedIds }
                    .take(15)

                trending = api.searchSongs(
                    "trending hit songs India",
                    20
                )
                    .filterNot { it.isArtist }
                    .distinctBy { it.videoId }
                    .take(12)

                popularArtists = api.searchSongs(
                    "popular Indian music artists",
                    20
                )
                    .filter { it.isArtist }
                    .distinctBy { it.title }
                    .take(10)

            } catch (e: Exception) {
                error = e.message ?: "Unable to load music"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRecommendations()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "MUSIC NOVA",
                    color = HomeAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Discover your sound",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { loadRecommendations() }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = HomeAccent
                )
            }
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = HomeAccent
            )
        }

        error?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 13.sp
            )
        }
        
        if (recommendations.isNotEmpty()) {
            MusicSection(
                title = "Recommended for you",
                songs = recommendations,
                onPlay = onPlay
            )
        }

        if (trending.isNotEmpty()) {
            MusicSection(
                title = "Trending music",
                songs = trending,
                onPlay = onPlay
            )
        }

        if (popularArtists.isNotEmpty()) {
            MusicSection(
                title = "Popular Artists",
                songs = popularArtists,
                onPlay = onPlay
            )
        }

        if (recentlyPlayed.isNotEmpty()) {
            Text(
                "Recently Played",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            recentlyPlayed.forEach { song ->
                Text(
                    text = "${song.title} • ${song.artist}",
                    color = HomeMuted,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun MusicSection(
    title: String,
    songs: List<YouTubeMusicTrack>,
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = songs,
                key = { it.videoId }
            ) { song ->

                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { onPlay(song) }
                ) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = song.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    Text(
                        text = song.artist,
                        color = HomeMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

    
