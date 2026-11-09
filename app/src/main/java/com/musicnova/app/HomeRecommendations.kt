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
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.ui.home.MusicViewModel

private val HomeBg = Color(0xFF09080E)
private val HomeSurface = Color(0xFF17141E)
private val HomeSurface2 = Color(0xFF211B2B)
private val HomeLavender = Color(0xFFD1C1EB)
private val HomeText = Color(0xFFF7F4FA)
private val HomeMuted = Color(0xFFAAA4B5)
private val HomeBorder = Color(0xFF393044)

@Composable
fun RecommendationsHomeScreen(
    onPlay: (YouTubeMusicTrack) -> Unit,
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

    val featured = recommendations.firstOrNull() ?: trending.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg),
        contentPadding = PaddingValues(top = 4.dp, bottom = 30.dp)
    ) {
        item(key = "header") {
            Row(
                Modifier
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

                if (refreshing) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = HomeLavender,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            "Refresh music",
                            tint = HomeLavender
                        )
                    }
                }
            }
        }

        item(key = "greeting") {
            Column(
                Modifier.padding(
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

        item(key = "stats") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(HomeSurface)
                    .padding(horizontal = 10.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeStat(
                    Icons.Default.Favorite,
                    likedCount.toString(),
                    "Liked songs",
                    Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(HomeBorder)
                )
                HomeStat(
                    Icons.Default.History,
                    recentlyPlayed.size.toString(),
                    "Recently played",
                    Modifier.weight(1f)
                )
            }
        }

        item(key = "spotlight") {
            Box(
                Modifier
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
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xAA20192C),
                                    Color(0xDD17121F),
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
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
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
                            Modifier
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

                            Column(Modifier.weight(1f)) {
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
                                "Play featured track",
                                tint = HomeLavender,
                                modifier = Modifier.size(35.dp)
                            )
                        }
                    } else {
                        Text(
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

        item(key = "quick_title") {
            HomeSectionTitle(
                "Your listening",
                "Your music, always close",
                Modifier.padding(top = 9.dp, bottom = 12.dp)
            )
        }

        item(key = "quick_cards") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                HomeQuickCard(
                    "Liked songs",
                    "$likedCount songs",
                    Icons.Default.Favorite,
                    Modifier.weight(1f)
                )
                HomeQuickCard(
                    "History",
                    "${recentlyPlayed.size} played",
                    Icons.Default.History,
                    Modifier.weight(1f)
                )
            }
        }

        item(key = "mix_title") {
            HomeSectionTitle(
                "Your Mix",
                "Music picked from your listening taste",
                Modifier.padding(top = 27.dp, bottom = 12.dp)
            )
        }

        if (recommendations.isEmpty() && !refreshing) {
            item(key = "mix_empty") {
                HomeEmptyMessage(
                    if (loadFailed) {
                        "Couldn't load recommendations. Try refreshing."
                    } else {
                        "Play some music to build your personal mix."
                    }
                )
            }
        }

        items(
            recommendations,
            key = { "mix_${it.videoId}" }
        ) { track ->
            HomeTrackRow(
                track.title,
                track.artist,
                track.artworkUrl
            ) {
                onPlay(track)
            }
        }

        item(key = "trending_title") {
            HomeSectionTitle(
                "Trending now",
                "Popular music right now",
                Modifier.padding(top = 27.dp, bottom = 13.dp)
            )
        }

        item(key = "trending_shelf") {
            if (trending.isEmpty() && !refreshing) {
                HomeEmptyMessage(
                    "Trending songs are not available right now."
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        item(key = "recent_title") {
            HomeSectionTitle(
                "Jump back in",
                "Recently played music",
                Modifier.padding(top = 27.dp, bottom = 12.dp)
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
                song.title,
                song.artist,
                song.artworkUrl
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

        item(key = "footer") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "MADE FOR YOUR MOMENTS",
                    color = HomeMuted,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(5.dp))
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
private fun HomeStat(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
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
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeSectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 20.dp)) {
        Text(
            title,
            color = HomeText,
            fontSize = 20.sp,
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
private fun HomeQuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HomeSurface)
            .padding(horizontal = 13.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(HomeSurface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = HomeLavender,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(9.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = HomeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                subtitle,
                color = HomeMuted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
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
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
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
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HomeSurface2)
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
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
            "Play",
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
        Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(20.dp))
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
                .height(132.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(HomeSurface2)
        )

        Spacer(Modifier.height(9.dp))

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
private fun HomeEmptyMessage(message: String) {
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
