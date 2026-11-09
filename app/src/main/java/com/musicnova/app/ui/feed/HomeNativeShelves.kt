
package com.musicnova.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.HomeShelf
import com.musicnova.app.data.music.HomeShelfItem
import com.musicnova.app.data.music.YouTubeMusicTrack

private val ShelfBackground = Color(0xFF17141E)
private val ShelfCardBackground = Color(0xFF211B2B)
private val ShelfText = Color(0xFFF7F4FA)
private val ShelfMuted = Color(0xFFAAA4B5)
private val ShelfAccent = Color(0xFFD1C1EB)

@Composable
fun HomeNativeShelves(
    shelves: List<HomeShelf>,
    onPlay: (YouTubeMusicTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        shelves.forEachIndexed { index, shelf ->
            if (shelf.items.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 20.dp
                        )
                    ) {
                        Text(
                            text = shelf.title,
                            color = ShelfText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        shelf.subtitle?.takeIf {
                            it.isNotBlank()
                        }?.let {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = it,
                                color = ShelfMuted,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        shelf.strapline?.takeIf {
                            it.isNotBlank()
                        }?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = it,
                                color = ShelfAccent,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(13.dp))

                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp
                        ),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        items(shelf.items) { item ->
                            HomeNativeShelfCard(
                                item = item,
                                onPlay = onPlay
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeNativeShelfCard(
    item: HomeShelfItem,
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    val title: String
    val subtitle: String
    val artwork: String?
    val playableTrack: YouTubeMusicTrack?

    when (item) {
        is HomeShelfItem.Track -> {
            title = item.track.title
            subtitle = item.track.artist
            artwork = item.cardData?.largeArtworkUrl
                ?: item.track.artworkUrl
            playableTrack = item.track
        }

        is HomeShelfItem.Playlist -> {
            title = item.playlist.title
            subtitle = item.playlist.author
                ?: item.playlist.trackCountText
                ?: "Playlist"
            artwork = item.cardData?.largeArtworkUrl
                ?: item.collageArtworkUrls.firstOrNull()
                ?: item.playlist.artworkUrl
            playableTrack = item.previewSongs.firstOrNull()
        }

        is HomeShelfItem.PodcastEpisode -> {
            title = item.title
            subtitle = item.showName
                ?: item.subtitle
                ?: "Podcast episode"
            artwork = item.cardData?.largeArtworkUrl
                ?: item.artworkUrl
                ?: item.thumbnailUrls.firstOrNull()

            playableTrack = item.videoId.takeIf {
                it.isNotBlank()
            }?.let {
                YouTubeMusicTrack(
                    videoId = it,
                    title = item.title,
                    artist = item.showName
                        ?: "Podcast",
                    artworkUrl = artwork,
                    isArtist = false
                )
            }
        }

        is HomeShelfItem.PodcastShow -> {
            title = item.title
            subtitle = item.author ?: "Podcast"
            artwork = item.cardData?.largeArtworkUrl
                ?: item.artworkUrl
            playableTrack = null
        }
    }

    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(ShelfBackground)
            .clickable(
                enabled = playableTrack != null
            ) {
                playableTrack?.let(onPlay)
            }
            .padding(9.dp)
    ) {
        AsyncImage(
            model = artwork,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(138.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ShelfCardBackground)
        )

        Spacer(Modifier.height(9.dp))

        Text(
            text = title,
            color = ShelfText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = subtitle,
            color = ShelfMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
