package com.musicnova.app.data.music

import org.json.JSONArray
import org.json.JSONObject

internal object PlaylistBrowseParser {

    fun parse(root: JSONObject): HomePage {
        val tracks = mutableListOf<HomeShelfItem>()
        val seenIds = mutableSetOf<String>()

        val rendererKeys = listOf(
            "musicResponsiveListItemRenderer",
            "playlistPanelVideoRenderer",
            "playlistVideoRenderer",
            "videoRenderer",
            "compactVideoRenderer"
        )

        for (key in rendererKeys) {
            for (renderer in findObjects(root, key)) {
                val track = parseTrack(renderer) ?: continue

                if (track.videoId.isBlank()) continue
                if (!seenIds.add(track.videoId)) continue

                tracks.add(HomeShelfItem.Track(track))
            }
        }

        val header = findObjects(
            root,
            "musicResponsiveHeaderRenderer"
        ).firstOrNull()
            ?: findObjects(
                root,
                "musicDetailHeaderRenderer"
            ).firstOrNull()
            ?: findObjects(
                root,
                "playlistHeaderRenderer"
            ).firstOrNull()

        val playlistTitle = header?.let {
            readText(it.optJSONObject("title"))
                ?: readText(it.optJSONObject("titleText"))
        } ?: "Playlist"

        val shelf = HomeShelf(
            title = playlistTitle,
            subtitle = "Community Playlist",
            items = tracks,
            sourceItemCount = tracks.size
        )

        return HomePage(
            shelves = if (tracks.isEmpty()) {
                emptyList()
            } else {
                listOf(shelf)
            },
            chips = emptyList(),
            continuationToken = findString(
                root,
                "continuation"
            ),
            backgroundUrl = null
        )
    }

    private fun parseTrack(
        row: JSONObject
    ): YouTubeMusicTrack? {

        val videoId = findString(row, "videoId")
            ?: return null

        val columns = row.optJSONArray("flexColumns")

        val title = columns
            ?.optJSONObject(0)
            ?.optJSONObject(
                "musicResponsiveListItemFlexColumnRenderer"
            )
            ?.optJSONObject("text")
            ?.let(::readText)
            ?: readText(row.optJSONObject("title"))
            ?: readText(row.optJSONObject("titleText"))
            ?: return null

        val artist = columns
            ?.optJSONObject(1)
            ?.optJSONObject(
                "musicResponsiveListItemFlexColumnRenderer"
            )
            ?.optJSONObject("text")
            ?.let(::readText)
            ?: readText(row.optJSONObject("longBylineText"))
            ?: readText(row.optJSONObject("shortBylineText"))
            ?: readText(row.optJSONObject("ownerText"))
            ?: "Unknown Artist"

        return YouTubeMusicTrack(
            videoId = videoId,
            title = title,
            artist = artist,
            artworkUrl = thumbnailUrl(row),
            isArtist = false
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
                    node.optJSONObject(target)?.let {
                        result.add(it)
                    }

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
        private fun readText(
        value: Any?
    ): String? {
        when (value) {
            is String -> {
                return value.takeIf { it.isNotBlank() }
            }

            is JSONObject -> {
                val simpleText = value.optString("simpleText")
                if (simpleText.isNotBlank() && simpleText != "null") {
                    return simpleText
                }

                val runs = value.optJSONArray("runs")
                if (runs != null) {
                    val combined = buildString {
                        for (i in 0 until runs.length()) {
                            append(
                                readText(runs.optJSONObject(i))
                                    .orEmpty()
                            )
                        }
                    }

                    if (combined.isNotBlank()) return combined
                }

                val text = value.optString("text")
                if (text.isNotBlank() && text != "null") {
                    return text
                }
            }

            is JSONArray -> {
                val combined = buildString {
                    for (i in 0 until value.length()) {
                        append(readText(value.opt(i)).orEmpty())
                    }
                }

                return combined.takeIf { it.isNotBlank() }
            }
        }

        return null
    }

    private fun findString(
        value: Any?,
        target: String
    ): String? {
        when (value) {
            is JSONObject -> {
                val direct = value.opt(target)

                if (direct is String &&
                    direct.isNotBlank() &&
                    direct != "null"
                ) {
                    return direct
                }

                val keys = value.keys()
                while (keys.hasNext()) {
                    val result = findString(
                        value.opt(keys.next()),
                        target
                    )

                    if (!result.isNullOrBlank()) return result
                }
            }

            is JSONArray -> {
                for (i in 0 until value.length()) {
                    val result = findString(
                        value.opt(i),
                        target
                    )

                    if (!result.isNullOrBlank()) return result
                }
            }
        }

        return null
    }

    private fun thumbnailUrl(
        value: Any?
    ): String? {
        when (value) {
            is JSONObject -> {
                val thumbnails = value.optJSONArray("thumbnails")

                if (thumbnails != null) {
                    for (i in thumbnails.length() - 1 downTo 0) {
                        val url = thumbnails.optJSONObject(i)
                            ?.optString("url")
                            .orEmpty()

                        if (url.startsWith("https://") ||
                            url.startsWith("http://")
                        ) {
                            return url
                        }
                    }
                }

                val keys = value.keys()
                while (keys.hasNext()) {
                    val result = thumbnailUrl(
                        value.opt(keys.next())
                    )

                    if (!result.isNullOrBlank()) return result
                }
            }

            is JSONArray -> {
                for (i in 0 until value.length()) {
                    val result = thumbnailUrl(value.opt(i))
                    if (!result.isNullOrBlank()) return result
                }
            }
        }

        return null
    }
}
