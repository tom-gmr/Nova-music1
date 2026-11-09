package com.musicnova.app.ui.feed.cards

import com.musicnova.app.data.music.HomeShelf
import com.musicnova.app.data.music.HomeShelfItem

enum class HomeCardVariant {
    SQUARE,
    WIDE,
    FEATURED,
    COMMUNITY,
    PODCAST_EPISODE
}

fun selectCardVariant(
    item: HomeShelfItem,
    shelf: HomeShelf? = null
): HomeCardVariant {

    if (
        shelf != null &&
        isFeaturedShelf(shelf) &&
        item in shelf.items
    ) {
        return HomeCardVariant.FEATURED
    }

    return when (item) {
        is HomeShelfItem.Track -> {
            if (isWideAspect(item.aspectRatio)) {
                HomeCardVariant.WIDE
            } else {
                HomeCardVariant.SQUARE
            }
        }

        is HomeShelfItem.Playlist -> {
            when {
                isCommunityPlaylistItem(item) ->
                    HomeCardVariant.COMMUNITY

                isWideAspect(item.aspectRatio) ->
                    HomeCardVariant.WIDE

                else -> HomeCardVariant.SQUARE
            }
        }

        is HomeShelfItem.PodcastEpisode ->
            HomeCardVariant.PODCAST_EPISODE

        is HomeShelfItem.PodcastShow ->
            HomeCardVariant.SQUARE
    }
}

fun isFeaturedShelf(shelf: HomeShelf): Boolean {
    val description = listOfNotNull(
        shelf.title,
        shelf.strapline
    ).joinToString(" ").lowercase()

    return description.contains("daily discover") ||
        description.contains("your daily discovery")
}

fun isCommunityPlaylistItem(
    item: HomeShelfItem.Playlist
): Boolean {
    val type = item.itemType.orEmpty()

    return type.contains("community", ignoreCase = true) ||
        (
            item.collageArtworkUrls.isNotEmpty() &&
            item.previewSongs.isNotEmpty()
        )
}

fun isWideAspect(aspectRatio: String?): Boolean {
    return aspectRatio?.contains(
        "RECTANGLE",
        ignoreCase = true
    ) == true
}

fun shelfIsPlayableTracks(
    items: List<HomeShelfItem>
): Boolean {
    return items.isNotEmpty() &&
        items.all { it is HomeShelfItem.Track }
}
