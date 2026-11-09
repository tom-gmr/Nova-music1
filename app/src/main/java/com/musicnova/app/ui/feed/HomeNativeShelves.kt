package com.musicnova.app.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicnova.app.data.music.HomeShelf
import com.musicnova.app.data.music.HomeShelfItem
import com.musicnova.app.data.music.PlaylistPreviewState
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.ui.feed.cards.*
import kotlinx.coroutines.flow.collect

@Composable
fun HomeNativeShelves(
    shelves: List<HomeShelf>,
    onPlay: (YouTubeMusicTrack) -> Unit,
    modifier: Modifier = Modifier,
    previewViewModel: PlaylistPreviewViewModel = hiltViewModel()
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
                        items(
                            items = shelf.items,
                            key = { item ->
                                when (item) {
                                    is HomeShelfItem.Track ->
                                        "track_${item.track.videoId}"

                                    is HomeShelfItem.Playlist ->
                                        "playlist_${item.playlist.id}"

                                    is HomeShelfItem.PodcastEpisode ->
                                        "episode_${item.videoId}"

                                    is HomeShelfItem.PodcastShow ->
                                        "show_${item.title}"
                                }
                            }
                        ) { item ->
                            NativeShelfCard(
                                item = item,
                                shelf = shelf,
                                onPlay = onPlay,
                                previewViewModel = previewViewModel
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
    onPlay: (YouTubeMusicTrack) -> Unit,
    previewViewModel: PlaylistPreviewViewModel
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
            val playlistId = playlist.playlist.id

            var previewState by remember(playlistId) {
                mutableStateOf<PlaylistPreviewState>(
                    PlaylistPreviewState.Loading
                )
            }

            LaunchedEffect(playlistId) {
                previewViewModel.load(playlistId).collect { state ->
                    previewState = state
                }
            }

            val readyPreview =
                (previewState as? PlaylistPreviewState.Ready)?.preview

            val previewSongs = readyPreview?.songs
                ?: playlist.previewSongs

            val collageUrls = readyPreview?.collageArtworkUrls
                ?.takeIf { it.isNotEmpty() }
                ?: playlist.collageArtworkUrls

            val previewLoading =
                previewState is PlaylistPreviewState.Loading

            val previewFailed =
                previewState is PlaylistPreviewState.Failed

            ShelfCommunityPlaylistCard(
                playlistId = playlistId,
                title = readyPreview?.title
                    ?: playlist.playlist.title,
                subtitle = listOfNotNull(
                    playlist.playlist.author,
                    readyPreview?.songCount?.let { "$it songs" }
                        ?: item.cardData?.viewsText,
                    playlist.playlist.trackCountText
                ).joinToString(" • "),
                collageUrls = collageUrls,
                songs = previewSongs,
                artworkUrl = item.cardData?.largeArtworkUrl
                    ?: playlist.playlist.artworkUrl,
                onClick = {
                    previewSongs.firstOrNull()?.let(onPlay)
                },
                onPlayClick = {
                    previewSongs.firstOrNull()?.let(onPlay)
                },
                onSongClick = onPlay,
                isLoading = previewLoading,
                previewFailed = previewFailed,
                songPlayCounts = readyPreview?.songPlayCounts
                    ?: emptyMap()
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
