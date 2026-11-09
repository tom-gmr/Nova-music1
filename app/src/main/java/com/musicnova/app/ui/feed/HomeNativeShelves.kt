
package com.musicnova.app.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicnova.app.data.music.HomeShelf
import com.musicnova.app.data.music.HomeShelfItem
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.ui.feed.cards.*

@Composable
fun HomeNativeShelves(
    shelves: List<HomeShelf>,
    onPlay: (YouTubeMusicTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        shelves.forEach { shelf ->
            if (shelf.items.isNotEmpty()) {
                val tracks = shelf.items
                    .filterIsInstance<HomeShelfItem.Track>()
                    .map { it.track }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                ) {
                    ShelfHeaderV2(
                        title = shelf.title,
                        eyebrow = shelf.strapline,
                        subtitle = shelf.subtitle,
                        onShuffle = if (tracks.isNotEmpty()) {
                            { onPlay(tracks.shuffled().first()) }
                        } else null
                    )

                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(
                            CardTokens.cardGap
                        ),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        items(shelf.items) { item ->
                            NativeShelfCard(
                                item = item,
                                shelf = shelf,
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
private fun NativeShelfCard(
    item: HomeShelfItem,
    shelf: HomeShelf,
    onPlay: (YouTubeMusicTrack) -> Unit
) {
    when (selectCardVariant(item, shelf)) {

        HomeCardVariant.FEATURED -> {
            val track = (item as? HomeShelfItem.Track)?.track
            if (track != null) {
                ShelfFeaturedCard(
                    title = track.title,
                    artist = listOfNotNull(
                        track.artist.takeIf { it.isNotBlank() },
                        item.cardData?.playsText
                    ).joinToString(" • "),
                    artworkUrl = item.cardData?.largeArtworkUrl
                        ?: track.artworkUrl,
                    caption = item.caption,
                    onClick = { onPlay(track) }
                )
            }
        }

        HomeCardVariant.COMMUNITY -> {
            val playlist = item as HomeShelfItem.Playlist
            val preview = playlist.previewSongs

            ShelfCommunityPlaylistCard(
                playlistId = playlist.playlist.id,
                title = playlist.playlist.title,
                subtitle = listOfNotNull(
                    playlist.playlist.author,
                    item.cardData?.viewsText,
                    playlist.playlist.trackCountText
                ).joinToString(" • "),
                collageUrls = playlist.collageArtworkUrls,
                songs = preview,
                artworkUrl = item.cardData?.largeArtworkUrl
                    ?: playlist.playlist.artworkUrl,
                onClick = {
                    preview.firstOrNull()?.let(onPlay)
                },
                onPlayClick = {
                    preview.firstOrNull()?.let(onPlay)
                },
                onSongClick = onPlay
            )
        }

        HomeCardVariant.WIDE -> {
            val title: String
            val subtitle: String
            val artwork: String?
            val playable: YouTubeMusicTrack?

            when (item) {
                is HomeShelfItem.Track -> {
                    title = item.track.title
                    subtitle = item.track.artist
                    artwork = item.cardData?.largeArtworkUrl
                        ?: item.track.artworkUrl
                    playable = item.track
                }

                is HomeShelfItem.Playlist -> {
                    title = item.playlist.title
                    subtitle = item.playlist.author
                        ?: item.playlist.trackCountText.orEmpty()
                    artwork = item.cardData?.largeArtworkUrl
                        ?: item.playlist.artworkUrl
                    playable = item.previewSongs.firstOrNull()
                }

                else -> return
            }

            ShelfWideCard(
                title = title,
                infoLine = subtitle,
                artworkUrl = artwork,
                onClick = { playable?.let(onPlay) }
            )
        }

        HomeCardVariant.PODCAST_EPISODE -> {
            val episode = item as HomeShelfItem.PodcastEpisode

            val track = YouTubeMusicTrack(
                videoId = episode.videoId,
                title = episode.title,
                artist = episode.showName ?: "Podcast",
                artworkUrl = episode.cardData?.largeArtworkUrl
                    ?: episode.artworkUrl
                    ?: episode.thumbnailUrls.firstOrNull(),
                isArtist = false
            )

            ShelfPodcastEpisodeCard(
                title = episode.title,
                infoLine = episode.showName
                    ?: episode.subtitle.orEmpty(),
                artworkUrl = track.artworkUrl,
                durationText = episode.durationText,
                progressPercentage = episode.progressFraction,
                onClick = { onPlay(track) }
            )
        }

        HomeCardVariant.SQUARE -> {
            val title: String
            val subtitle: String
            val artwork: String?
            val playable: YouTubeMusicTrack?

            when (item) {
                is HomeShelfItem.Track -> {
                    title = item.track.title
                    subtitle = item.track.artist
                    artwork = item.cardData?.largeArtworkUrl
                        ?: item.track.artworkUrl
                    playable = item.track
                }

                is HomeShelfItem.Playlist -> {
                    title = item.playlist.title
                    subtitle = item.playlist.author
                        ?: item.playlist.trackCountText.orEmpty()
                    artwork = item.cardData?.largeArtworkUrl
                        ?: item.collageArtworkUrls.firstOrNull()
                        ?: item.playlist.artworkUrl
                    playable = item.previewSongs.firstOrNull()
                }

                is HomeShelfItem.PodcastShow -> {
                    title = item.title
                    subtitle = item.author ?: "Podcast"
                    artwork = item.cardData?.largeArtworkUrl
                        ?: item.artworkUrl
                    playable = null
                }

                is HomeShelfItem.PodcastEpisode -> return
            }

            ShelfSquareCard(
                title = title,
                infoLine = subtitle,
                artworkUrl = artwork,
                onClick = { playable?.let(onPlay) }
            )
        }
    }
}
