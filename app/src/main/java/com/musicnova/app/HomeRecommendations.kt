
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
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.InnerTubeMusicApi
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
    var recommendations by remember {
        mutableStateOf<List<YouTubeMusicTrack>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val api = remember { InnerTubeMusicApi() }
    val scope = rememberCoroutineScope()

    fun loadRecommendations() {
        scope.launch {
            loading = true
            error = null

            try {
                val results = api.searchSongs("trending popular songs India")
                recommendations = results.filterNot { it.isArtist }.take(15)

                if (recommendations.isEmpty()) {
                    error = "No recommendations found. Try again."
                }
            } catch (e: Exception) {
                error = "Recommendations could not load. Check your internet."
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
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "YOUR SOUND, YOUR WORLD",
            color = HomeMuted,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Discover music.",
            color = Color.White,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Find something that feels right.",
            color = HomeMuted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF49385F),
                            Color(0xFF292238),
                            Color(0xFF17151E)
                        )
                    )
                )
                .padding(21.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    "FEEL THE SOUND",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    "A new favourite is waiting for you.",
                    color = Color(0xFFE0D6F0),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(13.dp))

                Text(
                    "MUSIC NOVA  •  DISCOVER",
                    color = HomeAccent,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }

        Spacer(Modifier.height(27.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Recommended for you",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Popular music from YouTube Music",
                    color = HomeMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { loadRecommendations() },
                enabled = !loading
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh recommendations",
                    tint = HomeAccent
                )
            }
        }

        Spacer(Modifier.height(13.dp))

        if (loading && recommendations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = HomeAccent,
                    strokeWidth = 2.dp
                )
            }
        }

        error?.let { message ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(17.dp))
                    .background(HomeCard)
                    .padding(16.dp)
            ) {
                Text(
                    message,
                    color = HomeMuted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Tap refresh to try again",
                    color = HomeAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        loadRecommendations()
                    }
                )
            }
        }

        if (recommendations.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                items(
                    recommendations,
                    key = { it.videoId }
                ) { song ->
                    RecommendationSongCard(
                        song = song,
                        onClick = { onPlay(song) }
                    )
                }
            }
        }

        Spacer(Modifier.height(27.dp))

        Text(
            "Trending music",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Explore songs people are listening to",
            color = HomeMuted,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(13.dp))

        recommendations.take(8).forEachIndexed { index, song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(HomeCard)
                    .clickable { onPlay(song) }
                    .padding(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    color = HomeMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.width(27.dp)
                )

                if (!song.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(49.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(49.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF30283C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = HomeAccent
                        )
                    }
                }

                Spacer(Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    Text(
                        song.artist,
                        color = HomeMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Icon(
                    Icons.Default.PlayCircleFilled,
                    contentDescription = "Play ${song.title}",
                    tint = HomeAccent,
                    modifier = Modifier.size(29.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RecommendationSongCard(
    song: YouTubeMusicTrack,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(145.dp)
            .clickable { onClick() }
    ) {
        Box {
            if (!song.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = song.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(17.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(HomeCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = HomeAccent,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Icon(
                Icons.Default.PlayCircleFilled,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(31.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            song.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        Text(
            song.artist,
            color = HomeMuted,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
