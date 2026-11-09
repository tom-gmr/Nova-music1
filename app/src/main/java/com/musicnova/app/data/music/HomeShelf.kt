
package com.musicnova.app.data.music

import androidx.compose.runtime.Immutable

/**
 * Represents an item in a native YouTube Music Home shelf.
 */
@Immutable
sealed interface HomeShelfItem {

    val aspectRatio: String?
        get() = null

    val itemType: String?
        get() = null

    val caption: String?
        get() = null

    val cardData: HomeCardData?
        get() = null

    @Immutable
    data class Track(
        val track: YouTubeMusicTrack,
        override val aspectRatio: String? = null,
        override val itemType: String? = null,
        override val caption: String? = null,
        override val cardData: HomeCardData? = null
    ) : HomeShelfItem

    @Immutable
    data class Playlist(
        val playlist: YouTubePlaylistSummary,
        override val aspectRatio: String? = null,
        override val itemType: String? = null,
        override val caption: String? = null,
        val collageArtworkUrls: List<String> = emptyList(),
        val previewSongs: List<YouTubeMusicTrack> = emptyList(),
        override val cardData: HomeCardData? = null
    ) : HomeShelfItem

    @Immutable
    data class PodcastEpisode(
        val videoId: String,
        val title: String,
        val showName: String? = null,
        val showBrowseId: String? = null,
        val subtitle: String? = null,
        val durationText: String? = null,
        val progressFraction: Float? = null,
        val artworkUrl: String? = null,
        val thumbnailUrls: List<String> = emptyList(),
        override val aspectRatio: String? =
            "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9",
        override val itemType: String? = "podcast_episode",
        override val caption: String? = null,
        override val cardData: HomeCardData? = null
    ) : HomeShelfItem

    @Immutable
    data class PodcastShow(
        val browseId: String,
        val title: String,
        val author: String? = null,
        val artworkUrl: String? = null,
        override val aspectRatio: String? =
            "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_SQUARE",
        override val itemType: String? = "podcast_show",
        override val caption: String? = null,
        override val cardData: HomeCardData? = null
    ) : HomeShelfItem
}

/**
 * Native YouTube Music Home shelf.
 */
@Immutable
data class HomeShelf(
    val title: String,
    val subtitle: String? = null,
    val items: List<HomeShelfItem> = emptyList(),
    val strapline: String? = null,
    val moreBrowseId: String? = null,
    val playAllButton: HomePlayAllButton? = null,
    val sourceItemCount: Int? = null
)
