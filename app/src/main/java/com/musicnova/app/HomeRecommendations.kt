package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.ui.feed.HomeNativeShelves
import com.musicnova.app.ui.home.MusicViewModel

private val HomeBg = Color(0xFF000000)
private val HomeSurface = Color(0xFF17141F)
private val HomeSurfaceLight = Color(0xFF211B2C)
private val HomeLavender = Color(0xFFD1C1EB)
private val HomeText = Color(0xFFF8F5FC)
private val HomeMuted = Color(0xFFAAA4B5)
private val HomeBorder = Color(0xFF25202D)

@Composable
fun RecommendationsHomeScreen(
    onPlay: (YouTubeMusicTrack) -> Unit,
    onBrowse: (String) -> Unit = {},
    viewModel: MusicViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val recommendations = uiState.recommendations
    val trending = uiState.trending
    val recentlyPlayed = uiState.recentlyPlayed
    val likedCount = uiState.likedCount
    val refreshing = uiState.isLoading
    val loadFailed = uiState.error != null

    LaunchedEffect(Unit) {
        viewModel.loadHomeMusic()
    }

    val featured = recommendations.firstOrNull()
        ?: trending.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg),
        contentPadding = PaddingValues(bottom = 35.dp)
    ) {

        item(key = "top_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 17.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(43.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF8870B4),
                                    Color(0xFF342744)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column {
                    Text(
                        "MUSIC NOVA",
                        color = HomeText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.3.sp
                    )

                    Text(
                        "YOUR MUSIC UNIVERSE",
                        color = HomeMuted,
                        fontSize = 9.sp,
                        letterSpacing = 1.8.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = HomeLavender,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = { viewModel.refresh() }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh music",
                            tint = HomeLavender
                        )
                    }
                }
            }
        }

        item(key = "welcome") {
            Column(
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 13.dp,
                    bottom = 17.dp
                )
            ) {
                Text(
                    "YOUR DAILY SOUNDTRACK",
                    color = HomeLavender,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    "Feel every\nmoment.",
                    color = HomeText,
                    fontSize = 35.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.2).sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "Discover the music that moves you.",
                    color = HomeMuted,
                    fontSize = 12.sp
                )
            }
        }

        item(key = "hero") {
            HomeHero(
                featured = featured,
                refreshing = refreshing,
                onPlay = onPlay
            )
        }

        item(key = "listening_stats") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 17.dp
                    )
                    .clip(RoundedCornerShape(21.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                HomeSurfaceLight,
                                HomeSurface
                            )
                        )
                    )
                    .padding(
                        horizontal = 15.dp,
                        vertical = 15.dp
                    ),
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

        item(key = "native_shelves") {
            val shelves = uiState.nativeHomePage?.shelves.orEmpty()

            if (shelves.isNotEmpty()) {
                HomeNativeShelves(
                    shelves = shelves,
                    onPlay = onPlay,
                    onBrowse = onBrowse,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // MADE FOR YOU BOX
        item(key = "mix_section") {
            HomeSectionBox(
                title = "Made for you",
                subtitle = "Your personal music mix"
            ) {
                if (recommendations.isEmpty()) {
                    HomeEmptyMessage(
                        if (loadFailed) {
                            "Couldn't load your mix. Try refreshing."
                        } else if (!refreshing) {
                            "Play music to build your personal mix."
                        } else {
                            "Finding music for you..."
                        }
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = 12.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        items(
                            recommendations.take(12),
                            key = { "mix_${it.videoId}" }
                        ) { track ->
                            HomeMusicCard(track) {
                                onPlay(track)
                            }
                        }
                    }
                }
            }
        }

        // TRENDING BOX
        item(key = "trending_section") {
            HomeSectionBox(
                title = "Trending now",
                subtitle = "Music people are listening to"
            ) {
                if (trending.isEmpty()) {
                    HomeEmptyMessage(
                        if (refreshing) {
                            "Finding trending music..."
                        } else {
                            "Trending music is not available right now."
                        }
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = 12.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        items(
                            trending,
                            key = { "trending_${it.videoId}" }
                        ) { track ->
                            HomeMusicCard(track) {
                                onPlay(track)
                            }
                        }
                    }
                }
            }
        }

        // RECENTLY PLAYED BOX
        item(key = "recent_section") {
            HomeSectionBox(
                title = "Jump back in",
                subtitle = "Continue where you left off"
            ) {
                if (recentlyPlayed.isEmpty()) {
                    HomeEmptyMessage(
                        "Your listening history will appear here."
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(
                            horizontal = 8.dp
                        )
                    ) {
                        recentlyPlayed.take(10).forEach { song ->
                            HomeTrackRow(
                                title = song.title,
                                artist = song.artist,
                                artwork = song.artworkUrl
                            ) {
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
                        }
                    }
                }
            }
        }

        item(key = "home_footer") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                        bottom = 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "MADE FOR YOUR MOMENTS",
                    color = HomeMuted,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "MUSIC NOVA",
                    color = HomeLavender,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun HomeSectionBox(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            )
            .clip(RoundedCornerShape(22.dp))
            .background(HomeBg)
            .border(
                width = 1.dp,
                color = HomeBorder,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(
                top = 13.dp,
                bottom = 13.dp
            )
    ) {
        HomeSectionHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.padding(
                bottom = 11.dp
            )
        )

        content()
    }
}

@Composable
private fun HomeHero(
    featured: YouTubeMusicTrack?,
    refreshing: Boolean,
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(300.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF55446C),
                        Color(0xFF30243D),
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
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x5520182B),
                            Color(0xCC17121F),
                            Color(0xFF17121F)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.TopEnd)
                .offset(x = 65.dp, y = (-65).dp)
                .clip(CircleShape)
                .background(
                    HomeLavender.copy(alpha = 0.12f)
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
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = HomeLavender,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(7.dp))

                Text(
                    "MUSIC NOVA EXCLUSIVE",
                    color = HomeLavender,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(Modifier.height(19.dp))

            Text(
                "FEEL THE\nSOUND.",
                color = Color.White,
                fontSize = 38.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.2).sp
            )

            Spacer(Modifier.height(7.dp))

            Text(
                "Let the music take you somewhere.",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 11.sp
            )

            Spacer(Modifier.weight(1f))

            if (featured != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            Color.White.copy(alpha = 0.13f)
                        )
                        .clickable {
                            onPlay(featured)
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = featured.artworkUrl,
                        contentDescription = featured.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
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
                    if (refreshing) {
                        "Finding music for you..."
                    } else {
                        "Your next                 Text(
                    if (refreshing) {
                        "Finding music for you..."
                    } else {
                        "Your next favourite song is waiting."
                    },
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HomeStat(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = HomeLavender,
            modifier = Modifier.size(19.dp)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            value,
            color = HomeText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            label,
            color = HomeMuted,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun HomeSectionHeader(
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
            fontWeight = FontWeight.ExtraBold
        )

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
            .width(153.dp)
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
                .height(137.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(HomeSurfaceLight)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            track.title,
            color = HomeText,
            fontSize = 11.sp,
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
private fun HomeTrackRow(
    title: String,
    artist: String,
    artwork: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(HomeSurface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artwork,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(51.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(HomeSurfaceLight)
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                color = HomeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            Text(
                artist,
                color = HomeMuted,
                fontSize = 10.sp,
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
private fun HomeEmptyMessage(
    message: String
) {
    Text(
        message,
        color = HomeMuted,
        fontSize = 11.sp,
        modifier = Modifier.padding(
            horizontal = 20.dp,
            vertical = 12.dp
        )
    )
}
