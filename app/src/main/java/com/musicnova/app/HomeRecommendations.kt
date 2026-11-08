
package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.*
import kotlinx.coroutines.launch

private val HomeBg = Color(0xFF09080E)
private val HomeSurface = Color(0xFF191620)
private val HomeSurface2 = Color(0xFF211C2A)
private val HomePurple = Color(0xFF9277B8)
private val HomeLavender = Color(0xFFD1C1EB)
private val HomeText = Color(0xFFF7F4FA)
private val HomeMuted = Color(0xFFAAA4B5)

@Composable
fun RecommendationsHomeScreen(
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val libraryStore = remember(context) {
        LibraryStore(context)
    }

    val api = remember { InnerTubeMusicApi() }
    val scope = rememberCoroutineScope()

    var recommendations by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }

    var trending by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }

    var recentlyPlayed by remember {
        mutableStateOf<List<LibraryTrack>>(emptyList())
    }

    var refreshing by remember {
        mutableStateOf(false)
    }

    fun loadMusic() {
        scope.launch {
            refreshing = true

            try {
                recentlyPlayed = libraryStore.getRecentlyPlayed()
                val liked = libraryStore.getLikedSongs()

                val seeds = (liked + recentlyPlayed)
                    .distinctBy { it.videoId }
                    .take(5)

                val results = mutableListOf<YouTubeMusicTrack>()

                if (seeds.isEmpty()) {
                    results.addAll(
                        api.searchSongs("popular trending songs India", 30)
                    )
                } else {
                    seeds.forEach { song ->
                        results.addAll(
                            api.searchSongs(
                                "songs like ${song.title} by ${song.artist}",
                                10
                            )
                        )
                    }
                }

                val excluded = (liked + recentlyPlayed)
                    .map { it.videoId }
                    .toSet()

                recommendations = results
                    .filterNot { it.isArtist }
                    .distinctBy { it.videoId }
                    .filterNot { it.videoId in excluded }
                    .take(15)

                trending = api.searchSongs(
                    "trending hit songs India",
                    20
                )
                    .filterNot { it.isArtist }
                    .distinctBy { it.videoId }
                    .take(12)

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    "MusicNova",
                    "Home music loading failed",
                    e
                )
            } finally {
                refreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMusic()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {

        // EXPRESSIVE HEADER

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "MUSIC NOVA",
                    color = HomeText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )

                Text(
                    "YOUR MUSIC UNIVERSE",
                    color = HomeMuted,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { loadMusic() },
                enabled = !refreshing
            ) {
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = HomeLavender,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = HomeLavender
                    )
                }
            }
        }

        // GREETING

        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                bottom = 20.dp
            )
        ) {
            Text(
                "Discover",
                color = HomeMuted,
                fontSize = 15.sp
            )

            Text(
                "Your sound.",
                color = HomeText,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
        }

        // FEATURED MUSIC CARD

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(250.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF514064),
                            Color(0xFF30263D),
                            Color(0xFF191620)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 55.dp, y = (-55).dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        HomeLavender.copy(alpha = 0.09f)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(21.dp)
            ) {
                Text(
                    "✦  MADE FOR YOU",
                    color = HomeLavender,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(Modifier.height(15.dp))

                Text(
                    "Feel every\nmoment.",
                    color = Color.White,
                    fontSize = 34.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.weight(1f))

                recommendations.firstOrNull()?.let { track ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Color.White.copy(alpha = 0.11f)
                            )
                            .clickable {
                                onPlay(track)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(49.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )

                        Spacer(Modifier.width(11.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "YOUR DAILY DISCOVERY",
                                color = HomeLavender,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                track.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                track.artist,
                                color = HomeMuted,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        Icon(
                            Icons.Default.PlayCircleFilled,
                            contentDescription = "Play",
                            tint = HomeLavender,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                } ?: Text(
                    if (refreshing) "Discovering music..." else "Refresh to discover music",
                    color = HomeMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        // SUGGESTED

        HomeSectionTitle(
            "Made for your taste",
            "Discover something new"
        )

        Spacer(Modifier.height(15.dp))

        if (recommendations.isEmpty() && !refreshing) {
            HomeEmptyMessage("No recommendations available yet.")
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                recommendations,
                key = { it.videoId }
            ) { track ->
                HomeMusicCard(
                    track = track,
                    onClick = { onPlay(track) }
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        // TRENDING

        HomeSectionTitle(
            "Trending now",
            "What everyone is listening to"
        )

        Spacer(Modifier.height(15.dp))

        if (trending.isEmpty() && !refreshing) {
            HomeEmptyMessage("Trending songs are not available right now.")
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                trending,
                key = { it.videoId }
            ) { track ->
                HomeMusicCard(
                    track = track,
                    onClick = { onPlay(track) }
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        // RECENTLY PLAYED

        HomeSectionTitle(
            "Recently played",
            "Pick up where you left off"
        )

        Spacer(Modifier.height(14.dp))

        if (recentlyPlayed.isEmpty()) {
            HomeEmptyMessage(
                "Your listening history will appear here."
            )
        } else {
            recentlyPlayed.take(8).forEach { song ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 17.dp,
                            vertical = 5.dp
                        )
                        .clip(RoundedCornerShape(19.dp))
                        .background(HomeSurface)
                        .clickable {
                            onPlay(
                                YouTubeMusicTrack(
                                    videoId = song.videoId,
                                    title = song.title,
                                    artist = song.artist,
                                    artworkUrl = song.artworkUrl,
                                    isArtist = false
                                )
                            )
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(15.dp))
                    )

                    Spacer(Modifier.width(13.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            song.title,
                            color = HomeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            song.artist,
                            color = HomeMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = HomeLavender
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HomeSectionTitle(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            title,
            color = HomeText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            subtitle,
            color = HomeMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HomeMusicCard(
    track: YouTubeMusicTrack,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(154.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(HomeSurface2)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .clip(RoundedCornerShape(17.dp))
        )

        Spacer(Modifier.height(11.dp))

        Text(
            track.title,
            color = HomeText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            track.artist,
            color = HomeMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeEmptyMessage(message: String) {
    Text(
        text = message,
        color = HomeMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(
            horizontal = 20.dp,
            vertical = 10.dp
        )
    )
}

