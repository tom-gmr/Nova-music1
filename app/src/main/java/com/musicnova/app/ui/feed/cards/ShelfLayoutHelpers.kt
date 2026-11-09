package com.musicnova.app.ui.feed.cards

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicnova.app.data.music.HomeShelfItem

/**
 * Generates stable and duplicate-safe keys for Home shelf items.
 */
internal fun stableShelfCardKeys(
    items: List<HomeShelfItem>
): List<String> {
    val occurrences = mutableMapOf<String, Int>()

    return items.map { item ->
        val id = when (item) {
            is HomeShelfItem.Track ->
                "track:${item.track.videoId}"

            is HomeShelfItem.Playlist ->
                "playlist:${item.playlist.id}"

            is HomeShelfItem.PodcastEpisode ->
                "episode:${item.videoId}"

            is HomeShelfItem.PodcastShow ->
                "show:${item.browseId}"
        }

        val occurrence = occurrences.getOrDefault(id, 0)
        occurrences[id] = occurrence + 1

        "$id:$occurrence"
    }
}

/**
 * Adaptive width used by large community and discovery shelves.
 */
internal fun largeShelfCardWidth(
    available: Dp
): Dp {
    return (available * 0.88f)
        .coerceIn(300.dp, 420.dp)
        .coerceAtMost(
            (available - 40.dp).coerceAtLeast(160.dp)
        )
}
