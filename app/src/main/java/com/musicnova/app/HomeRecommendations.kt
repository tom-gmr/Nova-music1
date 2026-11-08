package com.musicnova.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.musicnova.app.data.music.*
import kotlinx.coroutines.launch

private val NovaBlack = Color(0xFF08060D)
private val NovaPurple = Color(0xFF8438D5)
private val NovaPink = Color(0xFFE052C5)
private val NovaLavender = Color(0xFFC9A7FF)
private val NovaText = Color(0xFFF8F3FF)
private val NovaMuted = Color(0xFFC5B7D8)

private val NovaGlass = Color(0x55FFFFFF)
private val NovaBorder = Color(0x66FFFFFF)

private val HeroGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF762A91),
        Color(0xFF7539DB),
        Color(0xFFE34FC3)
    )
)

private val ScreenGradient = Brush.verticalGradient(
    colors = listOf(
        NovaBlack,
        Color(0xFF21102F),
        Color(0xFF55265F)
    )
)
@Composable
fun RecommendationsHomeScreen(
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    val context = LocalContext.current
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

    var refreshing by remember { mutableStateOf(false) }

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
                    seeds.forEach {
                        results.addAll(
                            api.searchSongs(
                                "songs like ${it.title} by ${it.artist}", 10
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
                    "trending hit songs India", 20
                )
                    .filterNot { it.isArtist }
                    .distinctBy { it.videoId }
                    .take(12)

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    "MusicNova",
                    "Recommendations loading failed",
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
            .background(ScreenGradient)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "MUSIC NOVA",
                    color = NovaText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "FEEL EVERY MOMENT",
                    color = NovaMuted,
                    fontSize = 10.sp,
                    letterSpacing = 3.sp
                )
            }

            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = "Notifications",
                tint = NovaText,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.height(25.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(205.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(HeroGradient)
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    "YOUR WORLD.",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "YOUR MUSIC.",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Discover your next favourite sound",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }

            Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(95.dp)
            )
        }
                Spacer(Modifier.height(28.dp))

        Text(
            "Suggested for You",
            color = NovaText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(recommendations) { track ->
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NovaGlass)
                        .clickable { onPlay(track) }
                        .padding(10.dp)
                ) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(135.dp)
                            .clip(RoundedCornerShape(15.dp))
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        track.title,
                        color = NovaText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        track.artist,
                        color = NovaMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Trending Now",
            color = NovaText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(14.dp))
                LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(trending) { track ->
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NovaGlass)
                        .clickable { onPlay(track) }
                        .padding(10.dp)
                ) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(135.dp)
                            .clip(RoundedCornerShape(15.dp))
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        track.title,
                        color = NovaText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        track.artist,
                        color = NovaMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(25.dp))

        Text(
            "Made for your mood",
            color = NovaText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Fresh music, selected for you.",
            color = NovaMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(20.dp))
    }
}

    
