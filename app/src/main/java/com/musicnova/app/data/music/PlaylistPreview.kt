
package com.musicnova.app.data.music

import org.json.JSONArray
import org.json.JSONObject

data class PlaylistPreview(
    val songs: List<YouTubeMusicTrack> = emptyList(),
    val collageArtworkUrls: List<String> = emptyList(),
    val songCount: Int? = null,
    val title: String? = null,
    val songPlayCounts: Map<String, String> = emptyMap()
)

internal object PlaylistPreviewParser {

    fun parse(root: JSONObject): PlaylistPreview? {
        val shelves = findObjects(root, "musicPlaylistShelfRenderer")
        val header = findObjects(root, "musicResponsiveHeaderRenderer")
            .firstOrNull()
            ?: findObjects(root, "musicDetailHeaderRenderer").firstOrNull()

        if (shelves.isEmpty() && header == null) return null

        val songs = mutableListOf<YouTubeMusicTrack>()
        val covers = linkedSetOf<String>()
        val playCounts = mutableMapOf<String, String>()

        for (shelf in shelves) {
            val contents = shelf.optJSONArray("contents") ?: continue

            for (i in 0 until contents.length()) {
                val row = contents.optJSONObject(i)
                    ?.optJSONObject("musicResponsiveListItemRenderer")
                    ?: continue

                val columns = row.optJSONArray("flexColumns")
                val firstText = columns?.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")

                val title = readText(firstText) ?: continue

                val firstRun = firstText?.optJSONArray("runs")
                    ?.optJSONObject(0)

                val videoId = findString(row, "videoId")
                    ?: findString(firstRun, "videoId")
                    ?: continue

                val artwork = thumbnailUrl(row)

                if (artwork != null && covers.size < 4) {
                    covers.add(artwork)
                }

                val secondText = columns?.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")

                val artist = readText(secondText) ?: "Unknown Artist"

                findPlayCount(row)?.let {
                    playCounts[videoId] = it
                }

                if (songs.size < 3) {
                    songs.add(
                        YouTubeMusicTrack(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            artworkUrl = artwork
                        )
                    )
                }
            }
        }

        val countText = header?.let {
            readText(it.optJSONObject("secondSubtitle"))
                ?: readText(it.optJSONObject("subtitle"))
        }

        val count = countText?.let {
            Regex(
                "([0-9][0-9,]*)\\s+(?:tracks?|songs?)",
                RegexOption.IGNORE_CASE
            ).find(it)?.groupValues?.get(1)
                ?.replace(",", "")
                ?.toIntOrNull()
        }

        val playlistTitle = header?.let {
            readText(it.optJSONObject("title"))
        }

        return PlaylistPreview(
            songs = songs,
            collageArtworkUrls = covers.toList(),
            songCount = count,
            title = playlistTitle,
            songPlayCounts = playCounts
        )
    }

    private fun findObjects(
        value: Any?,
        target: String
    ): List<JSONObject> {
        val result = mutableListOf<JSONObject>()

        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    val direct = node.optJSONObject(target)
                    if (direct != null) result.add(direct)

                    val keys = node.keys()
                    while (keys.hasNext()) {
                        walk(node.opt(keys.next()))
                    }
                }

                is JSONArray -> {
                    for (i in 0 until node.length()) {
                        walk(node.opt(i))
                    }
                }
            }
        }

        walk(value)
        return result
    }

    private fun readText(node: JSONObject?): String? {
        if (node == null) return null

        val simple = node.optString("simpleText")
        if (simple.isNotBlank() && simple != "null") {
            return simple.trim()
        }

        val runs = node.optJSONArray("runs") ?: return null

        val text = buildString {
            for (i in 0 until runs.length()) {
                append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
        }.trim()

        return text.takeIf { it.isNotBlank() }
    }

    private fun findString(
        value: Any?,
        target: String
    ): String? {
        when (value) {
            is JSONObject -> {
                val direct = value.optString(target)
                if (direct.isNotBlank() && direct != "null") {
                    return direct
                }

                val keys = value.keys()
                while (keys.hasNext()) {
                    findString(value.opt(keys.next()), target)?.let {
                        return it
                    }
                }
            }

            is JSONArray -> {
                for (i in 0 until value.length()) {
                    findString(value.opt(i), target)?.let {
                        return it
                    }
                }
            }
        }

        return null
    }

    private fun thumbnailUrl(value: Any?): String? {
        when (value) {
            is JSONObject -> {
                val thumbnails = value.optJSONArray("thumbnails")

                if (thumbnails != null) {
                    var bestUrl: String? = null
                    var bestWidth = -1

                    for (i in 0 until thumbnails.length()) {
                        val item = thumbnails.optJSONObject(i) ?: continue
                        val url = item.optString("url")
                        val width = item.optInt("width", 0)

                        if (url.isNotBlank() && url != "null" && width >= bestWidth) {
                            bestWidth = width
                            bestUrl = url
                        }
                    }

                    if (!bestUrl.isNullOrBlank()) {
                        return if (bestUrl.startsWith("//")) {
                            "https:$bestUrl"
                        } else {
                            bestUrl
                        }
                    }
                }

                val keys = value.keys()
                while (keys.hasNext()) {
                    thumbnailUrl(value.opt(keys.next()))?.let {
                        return it
                    }
                }
            }

            is JSONArray -> {
                for (i in 0 until value.length()) {
                    thumbnailUrl(value.opt(i))?.let {
                        return it
                    }
                }
            }
        }

        return null
    }

    private fun findPlayCount(row: JSONObject): String? {
        val columns = row.optJSONArray("flexColumns") ?: return null

        for (i in 0 until columns.length()) {
            val text = columns.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?: continue

            val runs = text.optJSONArray("runs") ?: continue

            for (j in 0 until runs.length()) {
                val value = runs.optJSONObject(j)?.optString("text")
                    ?.trim()
                    ?: continue

                if (value.endsWith(" plays", ignoreCase = true)) {
                    return value
                }
            }
        }

        return null
    }
}
