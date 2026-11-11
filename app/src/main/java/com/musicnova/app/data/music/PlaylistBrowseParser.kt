package com.musicnova.app.data.music

import org.json.JSONArray
import org.json.JSONObject

internal object PlaylistBrowseParser {

    fun parse(root: JSONObject): HomePage {
        val playlistShelves = findObjects(
            root,
            "musicPlaylistShelfRenderer"
        )

        val header = findObjects(
            root,
            "musicResponsiveHeaderRenderer"
        ).firstOrNull()
            ?: findObjects(
                root,
                "musicDetailHeaderRenderer"
            ).firstOrNull()

        val tracks = mutableListOf<HomeShelfItem>()
        val seenIds = mutableSetOf<String>()

        for (shelf in playlistShelves) {
            val contents = shelf.optJSONArray("contents")
                ?: continue

            for (i in 0 until contents.length()) {
                val row = contents.optJSONObject(i)
                    ?.optJSONObject(
                        "musicResponsiveListItemRenderer"
                    ) ?: continue

                val videoId = findString(row, "videoId")
                    ?: continue

                if (!seenIds.add(videoId)) continue

                val columns = row.optJSONArray("flexColumns")

                val title = columns
                    ?.optJSONObject(0)
                    ?.optJSONObject(
                        "musicResponsiveListItemFlexColumnRenderer"
                    )
                    ?.optJSONObject("text")
                    ?.let(::readText)
                    ?: readText(row.optJSONObject("title"))
                    ?: continue

                val artist = columns
                    ?.optJSONObject(1)
                    ?.optJSONObject(
                        "musicResponsiveListItemFlexColumnRenderer"
                    )
                    ?.optJSONObject("text")
                    ?.let(::readText)
                    ?: "Unknown Artist"

                tracks.add(
                    HomeShelfItem.Track(
                        track = YouTubeMusicTrack(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            artworkUrl = thumbnailUrl(row)
                        )
                    )
                )
            }
        }

        val playlistTitle = header?.let {
            readText(it.optJSONObject("title"))
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

        val runs = node.optJSONArray("runs")
            ?: return null

        val result = buildString {
            for (i in 0 until runs.length()) {
                append(
                    runs.optJSONObject(i)
                        ?.optString("text")
                        .orEmpty()
                )
            }
        }.trim()

        return result.takeIf { it.isNotBlank() }
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
                    findString(
                        value.opt(keys.next()),
                        target
                    )?.let { return it }
                }
            }

            is JSONArray -> {
                for (i in 0 until value.length()) {
                    findString(
                        value.opt(i),
                        target
                    )?.let { return it }
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
                        val item = thumbnails.optJSONObject(i)
                            ?: continue

                        val url = item.optString("url")
                        val width = item.optInt("width", 0)

                        if (
                            url.isNotBlank() &&
                            url != "null" &&
                            width >= bestWidth
                        ) {
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
}
