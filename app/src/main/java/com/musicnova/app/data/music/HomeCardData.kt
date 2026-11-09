package com.musicnova.app.data.music

import androidx.compose.runtime.Immutable

@Immutable
data class HomeCardData(
    val isTwoRow: Boolean = false,
    val browseId: String? = null,
    val pageType: String? = null,
    val firstSubtitleBrowseId: String? = null,
    val firstSubtitlePageType: String? = null,
    val watchVideoId: String? = null,
    val viewsText: String? = null,
    val playsText: String? = null,
    val largeArtworkUrl: String? = null,
)

@Immutable
data class HomePlayAllButton(
    val text: String? = null,
    val watchVideoId: String? = null
)
