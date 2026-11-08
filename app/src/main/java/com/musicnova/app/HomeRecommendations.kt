
package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.musicnova.app.data.music.*
import kotlinx.coroutines.launch

private val HomeBg = Color(0xFF09080E)
private val HomeSurface = Color(0xFF191620)
private val HomeSurface2 = Color(0xFF211C2A)
private val HomeLavender = Color(0xFFD1C1EB)
private val HomeText = Color(0xFFF7F4FA)
private val HomeMuted = Color(0xFFAAA4B5)

@Composable
fun RecommendationsHomeScreen(
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    val context = LocalContext.current
    val libraryStore = remember(context) { LibraryStore(context) }
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
    var likedCount by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    fun loadMusic() {
        if (refreshing) return

        scope.launch {
            refreshing = true

            try {
                recentlyPlayed = libraryStore.getRecentlyPlayed()
                val liked = libraryStore.getLikedSongs()
                likedCount = liked.size

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // HEADER
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "MUSIC NOVA",
                        color = HomeText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp
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
                            modifier = Modifier.size(20.dp),
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
        }

        // GREETING
        item(key = "greeting") {
            Column(
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 18.dp
                )
            ) {
                Text(
                    "WELCOME TO YOUR SOUND",
                    color = HomeMuted,
                    fontSize = 10.sp,
                    letterSpacing = 1.7.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Discover music.",
                    color = HomeText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp
                )
            }
        }

        // LISTENING SUMMARY
        item(key = "summary") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(HomeSurface)
                    .padding(vertical = 17.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeStat(
                    icon = Icons.Default.Favorite,
                    value = likedCount.toString(),
                    label = "Liked songs",
                    modifier = Modifier.weight(1f)
                )

                Box(
                    Modifier
                        .width(1.dp)
                        .height(35.dp)
                        .background(HomeMuted.copy(alpha = 0.2f))
                )

                HomeStat(
                    icon = Icons.Default.History,
                    value = recentlyPlayed.size.toString(),
                    label = "Recently played",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        
        // FEATURED CARD — NOVA / LASTWAVE INSPIRED HERO
        item(key = "featured") {
            val featured = recommendations.firstOrNull()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(238.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF514064),
                                Color(0xFF30243D),
                                Color(0xFF17131F)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 55.dp, y = (-55).dp)
                        .clip(CircleShape)
                        .background(
                            HomeLavender.copy(alpha = 0.10f)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = HomeLavender,
                            modifier = Modifier.size(17.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            "MUSIC NOVA  •  YOUR UNIVERSE",
                            color = HomeLavender,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(Modifier.height(17.dp))

                    Text(
                        "FEEL THE\nSOUND.",
                        color = Color.White,
                        fontSize = 35.sp,
                        lineHeight = 39.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        "Discover your next favourite track.",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.weight(1f))

                    if (featured != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(19.dp))
                                .background(
                                    Color.White.copy(alpha = 0.11f)
                                )
                                .clickable { onPlay(featured) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = featured.artworkUrl,
                                contentDescription = featured.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(13.dp))
                            )

                            Spacer(Modifier.width(11.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "TODAY'S DISCOVERY",
                                    color = HomeLavender,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    featured.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    featured.artist,
                                    color = HomeMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                Icons.Default.PlayCircleFilled,
                                contentDescription = "Play discovery",
                                tint = HomeLavender,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    } else {
                        Text(
                            if (refreshing) "Discovering music..."
                            else "Your next favourite song awaits.",
                            color = HomeMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        
        

        // RECOMMENDATIONS HEADER
        item(key = "recommendation_header") {
            HomeSectionTitle(
                title = "Your Mix",
                subtitle = "Music picked for your taste",
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            )
        }

        if (recommendations.isEmpty() && !refreshing) {
            item(key = "recommendation_empty") {
                HomeEmptyMessage("No recommendations available yet.")
            }
        }

        items(
            recommendations,
            key = { "mix_${it.videoId}" }
        ) { track ->
            HomeTrackRow(
                title = track.title,
                artist = track.artist,
                artwork = track.artworkUrl,
                onClick = { onPlay(track) }
            )
        }

        // TRENDING
        item(key = "trending_header") {
            HomeSectionTitle(
                title = "Trending now",
                subtitle = "Popular music right now",
                modifier = Modifier.padding(top = 25.dp, bottom = 13.dp)
            )
        }

        item(key = "trending_list") {
            if (trending.isEmpty() && !refreshing) {
                HomeEmptyMessage("Trending songs are not available right now.")
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            }
        }

        // RECENTLY PLAYED
        item(key = "recent_header") {
            HomeSectionTitle(
                title = "Recently played",
                subtitle = "Continue where you left off",
                modifier = Modifier.padding(top = 26.dp, bottom = 12.dp)
            )
        }

        if (recentlyPlayed.isEmpty()) {
            item(key = "recent_empty") {
                HomeEmptyMessage(
                    "Your listening history will appear here."
                )
            }
        }

        items(
            recentlyPlayed.take(8),
            key = { "recent_${it.videoId}" }
        ) { song ->
            HomeTrackRow(
                title = song.title,
                artist = song.artist,
                artwork = song.artworkUrl,
                onClick = {
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
            )
        }
    }
}

@Composable
private fun HomeStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = HomeLavender,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                value,
                color = HomeText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = HomeMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun HomeSectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            title,
            color = HomeText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
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
private fun HomeTrackRow(
    title: String,
    artist: String,
    artwork: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(HomeSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artwork,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(HomeSurface2)
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = HomeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                artist,
                color = HomeMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = HomeLavender,
            modifier = Modifier.size(24.dp)
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
            .width(148.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(HomeSurface2)
            .clickable(onClick = onClick)
            .padding(9.dp)
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(15.dp))
        )

        Spacer(Modifier.height(9.dp))

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
