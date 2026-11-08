
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val HomeBg = Color(0xFF09080E)
private val HomeSurface = Color(0xFF17141E)
private val HomeSurface2 = Color(0xFF211B2B)
private val HomeLavender = Color(0xFFD1C1EB)
private val HomeText = Color(0xFFF7F4FA)
private val HomeMuted = Color(0xFFAAA4B5)
private val HomeBorder = Color(0xFF393044)

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
    var loadFailed by remember { mutableStateOf(false) }

    fun loadMusic() {
        if (refreshing) return

        scope.launch {
            refreshing = true
            loadFailed = false

            try {
                recentlyPlayed = libraryStore.getRecentlyPlayed()
                val liked = libraryStore.getLikedSongs()
                likedCount = liked.size

                val seeds = (liked + recentlyPlayed)
                    .distinctBy { it.videoId }
                    .take(5)

                val resultList = mutableListOf<YouTubeMusicTrack>()

                if (seeds.isEmpty()) {
                    resultList.addAll(
                        api.searchSongs("popular trending songs India", 30)
                    )
                } else {
                    seeds.forEach { song ->
                        resultList.addAll(
                            api.searchSongs(
                                "songs like ${song.title} by ${song.artist}",
                                10
                            )
                        )
                    }
                }

                val excludedIds = (liked + recentlyPlayed)
                    .map { it.videoId }
                    .toSet()

                recommendations = resultList
                    .filterNot { it.isArtist || it.videoId.isBlank() }
                    .distinctBy { it.videoId }
                    .filterNot { it.videoId in excludedIds }
                    .take(15)

                trending = api.searchSongs(
                    "trending hit songs India",
                    20
                )
                    .filterNot { it.isArtist || it.videoId.isBlank() }
                    .distinctBy { it.videoId }
                    .take(12)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadFailed = true
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

    val featured = recommendations.firstOrNull()
        ?: trending.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg),
        contentPadding = PaddingValues(
            top = 4.dp,
            bottom = 30.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // HEADER
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "MUSIC NOVA",
                        color = HomeText,
                        fontSize = 21.sp,
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

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(HomeSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            color = HomeLavender,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { loadMusic() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh music",
                                tint = HomeLavender
                            )
                        }
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
                    top = 12.dp,
                    bottom = 20.dp
                )
            ) {
                Text(
                    "WELCOME TO YOUR SOUND",
                    color = HomeMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Discover music.",
                    color = HomeText,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp
                )
                Text(
                    "Made for the way you listen.",
                    color = HomeMuted,
                    fontSize = 12.sp
                )
            }
        }

        // LISTENING STATS
        item(key = "stats") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(HomeSurface)
                    .padding(horizontal = 10.dp, vertical = 17.dp),
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
                        .height(38.dp)
                        .background(HomeBorder)
                )

                HomeStat(
                    icon = Icons.Default.History,
                    value = recentlyPlayed.size.toString(),
                    label = "Recently played",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // FEATURED SPOTLIGHT
        item(key = "spotlight") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 17.dp)
                    .height(285.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF57466D),
                                Color(0xFF33283F),
                                Color(0xFF17131F)
                            )
                        )
                    )
            ) {
                if (featured?.artworkUrl != null) {
                    AsyncImage(
                        model = featured.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.25f)
                    )
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x4420192C),
                                    Color(0xCC17121F),
                                    Color(0xFF17121F)
                                )
                            )
                        )
                )

                Box(
                    Modifier
                        .size(210.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 75.dp, y = (-75).dp)
                        .clip(CircleShape)
                        .background(HomeLavender.copy(alpha = 0.10f))
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
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = HomeLavender,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "MUSIC NOVA SPOTLIGHT",
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
                        fontSize = 37.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        "Find something worth playing.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )

                    Spacer(Modifier.weight(1f))

                    if (featured != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(19.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { onPlay(featured) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = featured.artworkUrl,
                                contentDescription = featured.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(53.dp)
                                    .clip(RoundedCornerShape(13.dp))
                            )

                            Spacer(Modifier.width(11.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "FEATURED TRACK",
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
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                Icons.Default.PlayCircleFilled,
                                contentDescription = "Play featured track",
                                tint = HomeLavender,
                                modifier = Modifier.size(35.dp)
                            )
                        }
                    } else {
                        Text(
                            if (refreshing) "Finding music for you..."
                            else "Your next favourite song is waiting.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // QUICK PICKS
        item(key = "quick_picks_title") {
            HomeSectionTitle(
                title = "Your listening",
                subtitle = "Your music, always close",
                modifier = Modifier.padding(top = 9.dp, bottom = 12.dp)
            )
        }

        item(key = "quick_picks") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                HomeQuickCard(
                    title = "Liked songs",
                    subtitle = "$likedCount songs",
                    icon = Icons.Default.Favorite,
                    modifier = Modifier.weight(1f)
                )

                HomeQuickCard(
                    title = "History",
                    subtitle = "${recentlyPlayed.size} played",
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // PERSONAL MIX
        item(key = "mix_title") {
            HomeSectionTitle(
                title = "Your Mix",
                subtitle = "Music picked from your listening taste",
                modifier = Modifier.padding(top = 27.dp, bottom = 12.dp)
            )
        }

        if (recommendations.isEmpty() && !refreshing) {
            item(key = "mix_empty") {
                HomeEmptyMessage(
                    if (loadFailed) "Couldn't load recommendations. Try refreshing."
                    else "Play some music to build your personal mix."
                )
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

        // TRENDING SHELF
        item(key = "trending_title") {
            HomeSectionTitle(
                title = "Trending now",
                subtitle = "Popular music right now",
                modifier = Modifier.padding(top = 27.dp, bottom = 13.dp)
            )
        }

        item(key = "trending_shelf") {
            if (trending.isEmpty() && !refreshing) {
                HomeEmptyMessage("Trending songs are not available right now.")
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        trending,
                        key = { "trending_${it.videoId}" }
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
        item(key = "recent_title") {
            HomeSectionTitle(
                title = "Jump back in",
                subtitle = "Recently played music",
                modifier = Modifier.padding(top = 27.dp, bottom = 12.dp)
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
            recentlyPlayed.take(10),
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

        item(key = "home_footer") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        AsyncImage(
            model = artwork,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(53.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(HomeSurface2)
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
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
            modifier = Modifier.size(25.dp)
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
            .clip(RoundedCornerShape(21.dp))
            .background(HomeSurface)
            .clickable(onClick = onClick)
            .padding(9.dp)
    ) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(HomeSurface2)
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
            vertical = 12.dp
        )
    )
}
