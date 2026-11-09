package com.musicnova.app.ui.feed.cards

import com.musicnova.app.data.music.HomeShelf
import com.musicnova.app.data.music.HomeShelfItem

/**
 * Visual card variants supported by Music Nova Home shelves.
 */
enum class HomeCardVariant {
    SQUARE,
    WIDE,
    FEATURED,
    COMMUNITY,
    PODCAST_EPISODE
}

/**
 * Selects a card variant using the data currently available in Nova.
 *
 * Playlist and podcast variants will be enabled after their data models
 * and parser support are integrated.
 */
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
    }
}

/**
 * Detects a daily-discover shelf from its available title and strapline.
 */
fun isFeaturedShelf(shelf: HomeShelf): Boolean {
    val description = listOfNotNull(
        shelf.title,
        shelf.strapline
    ).joinToString(" ").lowercase()

    return description.contains("daily discover") ||
        description.contains("your daily discovery")
}

/**
 * Checks whether an aspect ratio represents a wide rectangle card.
 */
private fun isWideAspect(aspectRatio: String?): Boolean {
    return aspectRatio?.contains(
        "RECTANGLE",
        ignoreCase = true
    ) == true
}

/**
 * True when every item in the shelf is a playable track.
 */
fun shelfIsPlayableTracks(
    items: List<HomeShelfItem>
): Boolean {
    return items.isNotEmpty() &&
        items.all { it is HomeShelfItem.Track }
}
