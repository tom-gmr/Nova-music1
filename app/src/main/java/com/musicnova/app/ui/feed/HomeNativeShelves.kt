package com.musicnova.app.ui.feed

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior

import com.musicnova.app.data.music.HomeShelf
import com.musicnova.app.data.music.HomeShelfItem
import com.musicnova.app.data.music.PlaylistPreviewState
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.ui.feed.cards.*

@Composable
fun HomeNativeShelves(
    shelves: List<HomeShelf>,
    onPlay: (YouTubeMusicTrack) -> Unit,
    modifier: Modifier = Modifier,
    onBrowse: (String) -> Unit = {},
    previewViewModel: PlaylistPreviewViewModel = hiltViewModel()
) {
    Column(modifier = modifier.fillMaxWidth()) {

        shelves.forEach { shelf ->

            if (shelf.items.isNotEmpty()) {

                val tracks = shelf.items
                    .filterIsInstance<HomeShelfItem.Track>()
                    .map { it.track }

                val browseId = shelf.moreBrowseId
                    ?.takeIf { it.isNotBlank() }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF000000))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF25202D),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(vertical = 12.dp)
                ) {

                    ShelfHeaderV2(
                        title = shelf.title,
                        eyebrow = shelf.strapline,
                        subtitle = shelf.subtitle,
                        showMore = browseId != null,
                        onMoreClick = browseId?.let {
                            { onBrowse(it) }
                        },
                        onShuffle = if (tracks.isNotEmpty()) {
                            { onPlay(tracks.shuffled().first()) }
                        } else null
                    )

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        val rowState = rememberLazyListState()

                        val flingBehavior = rememberSnapFlingBehavior(
                            lazyListState = rowState,
                            snapPosition = SnapPosition.Start
                        )

                        val view = LocalView.current
                        val cardWidth = largeShelfCardWidth(maxWidth)

                        val stableKeys = remember(shelf.items) {
                            stableShelfCardKeys(shelf.items)
                        }

                        var rowVisible by remember {
                            mutableStateOf(false)
                        }

                        LazyRow(
                            state = rowState,
                            flingBehavior = flingBehavior,
                            contentPadding = PaddingValues(
                                horizontal = 16.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(
                                CardTokens.cardGap
                            ),
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .onGloballyPositioned {
                                    val bounds = it.boundsInWindow()

                                    rowVisible =
                                        bounds.height > 0f &&
                                            bounds.width > 0f &&
                                            bounds.bottom > 0f &&
                                            bounds.top < view.height
                                }
                        ) {

                            itemsIndexed(
                                items = shelf.items,
                                key = { index, _ -> stableKeys[index] }
                            ) { index, item ->

                                val isVisible by remember(index) {
                                    derivedStateOf {
                                        rowVisible &&
                                            rowState.layoutInfo
                                                .visibleItemsInfo
                                                .any { it.index == index }
                                    }
                                }

                                NativeShelfCard(
                                    item = item,
                                    shelf = shelf,
                                    onPlay = onPlay,
                                    onBrowse = onBrowse,
                                    previewViewModel = previewViewModel,
                                    isVisible = isVisible,
                                    cardWidth = cardWidth
                                )
                            }
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
    onBrowse: (String) -> Unit,
    previewViewModel: PlaylistPreviewViewModel,
    isVisible: Boolean,
    cardWidth: androidx.compose.ui.unit.Dp
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
                    onClick = { onPlay(track) },
                    cardWidth = cardWidth,
                    isVisible = isVisible
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

            LaunchedEffect(playlistId, isVisible) {

                if (isVisible) {

                    previewViewModel.load(playlistId).collect { state ->
                        previewState = state
                    }
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
                isVisible &&
                    previewState is PlaylistPreviewState.Loading &&
                    previewSongs.isEmpty()

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

                // Playlist cover opens the complete playlist
                onClick = {
    onBrowse(
        if (playlistId.startsWith("VL")) playlistId
        else "VL$playlistId"
    )
},

                // Play button plays the first available song
                onPlayClick = {
                    previewSongs.firstOrNull()?.let(onPlay)
                },

                // Individual song plays directly
                onSongClick = onPlay,

                isLoading = previewLoading,
                previewFailed = previewFailed,
                songPlayCounts = readyPreview?.songPlayCounts
                    ?: emptyMap(),
                cardWidth = cardWidth
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
                onClick = {

                    val playlist = item as? HomeShelfItem.Playlist

                    if (playlist != null) {
                        onBrowse(
    if (playlist.playlist.id.startsWith("VL")) {
        playlist.playlist.id
    } else {
        "VL${playlist.playlist.id}"
    }
)
                    } else {
                        playable?.let(onPlay)
                    }
                }
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
                onClick = {
    val playlist = item as? HomeShelfItem.Playlist

    if (playlist != null) {
        onBrowse(
            if (playlist.playlist.id.startsWith("VL")) {
                playlist.playlist.id
            } else {
                "VL${playlist.playlist.id}"
            }
        )
    } else {
        playable?.let(onPlay)
    }
                }
            )
        }
    }
}
