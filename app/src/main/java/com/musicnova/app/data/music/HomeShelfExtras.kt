
package com.musicnova.app.data.music

import org.json.JSONArray
import org.json.JSONObject

/**
 * Optional layout and metadata extracted from YouTube Music Home renderers.
 */
internal data class HomeItemExtras(
    val aspectRatio: String? = null,
    val itemType: String? = null,
    val caption: String? = null,
    val collageArtworkUrls: List<String> = emptyList(),
    val previewSongs: List<YouTubeMusicTrack> = emptyList()
)

internal const val ASPECT_RECTANGLE_16_9 =
    "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9"

internal const val ASPECT_RECTANGLE_16_9_V2 =
    "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_SIXTEEN_BY_NINE"

internal fun isWideAspect(aspectRatio: String?): Boolean =
    aspectRatio?.contains("RECTANGLE", ignoreCase = true) == true

internal fun parsePageType(navEndpoint: JSONObject?): String? {
    val browse = navEndpoint?.optJSONObject("browseEndpoint")
        ?: return null

    return browse
        .optJSONObject("browseEndpointContextSupportedConfigs")
        ?.optJSONObject("browseEndpointContextMusicConfig")
        ?.optString("pageType")
        ?.takeIf { !it.isNullOrBlank() && it != "null" }
}

internal fun pageTypeToItemType(pageType: String?): String? =
    when {
        pageType == null -> null
        pageType.contains("PLAYLIST", ignoreCase = true) -> "playlist"
        pageType.contains("ALBUM", ignoreCase = true) ||
            pageType.contains("AUDIOBOOK", ignoreCase = true) -> "album"
        pageType.contains("ARTIST", ignoreCase = true) -> "artist"
        pageType.contains("PODCAST_SHOW", ignoreCase = true) -> "podcast"
        pageType.contains("NON_MUSIC_AUDIO_TRACK", ignoreCase = true) -> "episode"
        else -> null
    }

internal fun parseItemCaption(item: JSONObject): String? {
    val cardCaption =
        item.optJSONObject("subtitleDescription")?.let(::runsText)
            ?: item.optJSONObject("headline")?.let(::runsText)

    if (!cardCaption.isNullOrBlank()) return cardCaption.trim()

    val caption =
        item.optJSONObject("caption")?.let(::runsText)
            ?: item.optJSONObject("overline")?.let(::runsText)

    return caption?.trim()?.takeIf { it.isNotBlank() }
}

private fun runsText(node: JSONObject): String? {
    val runs = node.optJSONArray("runs")

    if (runs != null) {
        val result = buildString {
            for (i in 0 until runs.length()) {
                append(
                    runs.optJSONObject(i)
                        ?.optString("text")
                        .orEmpty()
                )
            }
        }

        if (result.isNotBlank()) return result
    }

    return node.optString("simpleText")
        .takeIf { it.isNotBlank() && it != "null" }
}

internal fun parseCollageCovers(
    item: JSONObject,
    max: Int = 4
): List<String> {
    val collage =
        item.optJSONObject("thumbnailCollageRenderer")
            ?: item.optJSONObject("collageRenderer")
            ?: return emptyList()

    val output = mutableListOf<String>()
    collectThumbNodes(collage, output, max)

    return output.distinct().take(max)
}

private fun collectThumbNodes(
    value: Any?,
    output: MutableList<String>,
    max: Int
) {
    if (output.size >= max) return

    when (value) {
        is JSONObject -> {
            val thumbnails = value.optJSONArray("thumbnails")

            if (thumbnails != null && thumbnails.length() > 0) {
                val url = thumbnails
                    .optJSONObject(thumbnails.length() - 1)
                    ?.optString("url")
                    .orEmpty()

                if (url.isNotBlank() && url != "null") {
                    output.add(
                        if (url.startsWith("//")) "https:$url" else url
                    )
                }
            }

            val keys = value.keys()
            while (keys.hasNext() && output.size < max) {
                collectThumbNodes(value.opt(keys.next()), output, max)
            }
        }

        is JSONArray -> {
            for (i in 0 until value.length()) {
                if (output.size >= max) break
                collectThumbNodes(value.opt(i), output, max)
            }
        }
    }
}

internal fun parsePreviewSongs(
    item: JSONObject,
    parseSong: (JSONObject) -> YouTubeMusicTrack?,
    max: Int = 3
): List<YouTubeMusicTrack> {
    val rows = mutableListOf<JSONObject>()

    for (key in listOf("contents", "items", "songs", "previewTracks")) {
        val array = item.optJSONArray(key) ?: continue

        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue

            val renderer =
                entry.optJSONObject("musicResponsiveListItemRenderer")
                    ?: entry.optJSONObject("musicTwoRowItemRenderer")

            if (renderer != null) rows.add(renderer)
            if (rows.size >= max) break
        }

        if (rows.isNotEmpty()) break
    }

    return rows.mapNotNull(parseSong).take(max)
}

internal fun parseHomeItemExtras(
    item: JSONObject,
    navEndpoint: JSONObject?,
    parseSong: (JSONObject) -> YouTubeMusicTrack?
): HomeItemExtras {
    return HomeItemExtras(
        aspectRatio = item.optString("aspectRatio")
            .takeIf { it.isNotBlank() && it != "null" },
        itemType = pageTypeToItemType(parsePageType(navEndpoint)),
        caption = parseItemCaption(item),
        collageArtworkUrls = parseCollageCovers(item),
        previewSongs = parsePreviewSongs(item, parseSong)
    )
}
