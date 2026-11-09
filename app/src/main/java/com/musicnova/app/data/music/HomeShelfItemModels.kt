
package com.musicnova.app.data.music

import androidx.compose.runtime.Immutable

/**
 * YouTube Music playlist summary used by Home shelves.
 */
@Immutable
data class YouTubePlaylistSummary(
    val id: String,
    val title: String,
    val author: String? = null,
    val trackCountText: String? = null,
    val artworkUrl: String? = null
)

/**
 * Podcast episode information for Home cards.
 */
@Immutable
data class HomePodcastEpisode(
    val videoId: String,
    val title: String,
    val showName: String? = null,
    val showBrowseId: String? = null,
    val subtitle: String? = null,
    val durationText: String? = null,
    val progressFraction: Float? = null,
    val artworkUrl: String? = null,
    val thumbnailUrls: List<String> = emptyList()
)

/**
 * Podcast show information for Home cards.
 */
@Immutable
data class HomePodcastShow(
    val browseId: String,
    val title: String,
    val author: String? = null,
    val artworkUrl: String? = null
)
