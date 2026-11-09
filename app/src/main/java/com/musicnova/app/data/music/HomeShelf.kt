package com.musicnova.app.data.music

import androidx.compose.runtime.Immutable

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
}

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
