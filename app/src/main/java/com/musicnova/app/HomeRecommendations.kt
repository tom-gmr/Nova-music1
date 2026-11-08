
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
private val HomeCard = Color(0xFF1B1722)
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
            .padding(bottom = 24.dp)
    ) {

        // HEADER

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "MUSIC NOVA",
                    color = HomeText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Text(
                    "FEEL EVERY MOMENT",
                    color = HomeMuted,
                    fontSize = 10.sp,
                    letterSpacing = 2.5.sp
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
                        contentDescription = "Refresh music",
                        tint = HomeLavender
                    )
                }
            }
        }

        // HERO CARD

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(285.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF453452),
                            Color(0xFF292035),
                            Color(0xFF17131F)
                        )
                    )
                )
                .padding(23.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    "✦ YOUR SOUND, YOUR WORLD",
                    color = HomeLavender,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    "Feel the\nSound.",
                    color = Color.White,
                    fontSize = 39.sp,
                    lineHeight = 43.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    "Discover music that matches your mood.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )

                Spacer(Modifier.weight(1f))

                recommendations.firstOrNull()?.let { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { onPlay(track) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(53.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(Modifier.width(11.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "PICKED FOR YOU",
                                color = HomeLavender,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                track.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                track.artist,
                                color = HomeMuted,
                                fontSize = 11.sp,
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
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // SUGGESTED SONGS

        HomeSectionTitle(
            title = "Suggested for You",
            subtitle = "Music picked around your taste"
        )

        Spacer(Modifier.height(14.dp))

        if (recommendations.isEmpty() && !refreshing) {
            HomeEmptyMessage("No recommendations yet. Try refreshing.")
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
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
            title = "Trending Now",
            subtitle = "Popular music right now"
        )

        Spacer(Modifier.height(14.dp))

        if (trending.isEmpty() && !refreshing) {
            HomeEmptyMessage("Trending songs are not available right now.")
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
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
            title = "Recently Played",
            subtitle = "Continue where you left off"
        )

        Spacer(Modifier.height(12.dp))

        if (recentlyPlayed.isEmpty()) {
            HomeEmptyMessage("Your recently played songs will appear here.")
        } else {
            recentlyPlayed.take(6).forEach { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 17.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(HomeCard)
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
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(Modifier.width(12.dp))

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
                            maxLines = 1
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

        Spacer(Modifier.height(25.dp))
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
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(3.dp))

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
            .width(145.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(HomeCard)
            .clickable { onClick() }
            .padding(9.dp)
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(135.dp)
                .clip(RoundedCornerShape(14.dp))
        )

        Spacer(Modifier.height(10.dp))

        Text(
            track.title,
            color = HomeText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(3.dp))

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

