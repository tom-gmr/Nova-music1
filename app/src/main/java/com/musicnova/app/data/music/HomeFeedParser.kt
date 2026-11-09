package com.musicnova.app.data.music

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses native YouTube Music Home feed responses.
 */
internal object HomeFeedParser {

    fun parseHomePage(root: JSONObject): HomePage {
        return HomePage(
            shelves = parseShelves(root),
            chips = parseChips(root),
            continuationToken = parseContinuation(root),
            backgroundUrl = parseBackground(root)
        )
    }

    private fun parseShelves(root: JSONObject): List<HomeShelf> {
        val section = root
            .optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")

        val contents = section?.optJSONArray("contents")
            ?: return emptyList()

        val shelves = mutableListOf<HomeShelf>()

        for (i in 0 until contents.length()) {
            val carousel = contents.optJSONObject(i)
                ?.optJSONObject("musicCarouselShelfRenderer")
                ?: continue

            parseCarousel(carousel)?.let(shelves::add)
        }

        return shelves
    }

    private fun parseCarousel(
        carousel: JSONObject
    ): HomeShelf? {
        val header = carousel.optJSONObject("header")
            ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            ?: return null

        val title = readText(header.optJSONObject("title"))
            ?: return null

        val subtitle = readText(
            header.optJSONObject("strapline")
                ?: header.optJSONObject("subtitle")
        )

        val moreBrowseId = header
            .optJSONObject("moreContentButton")
            ?.optJSONObject("buttonRenderer")
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?.optString("browseId")
            ?.takeIf { !it.isNullOrBlank() && it != "null" }

        val contents = carousel.optJSONArray("contents")
            ?: return null

        val items = mutableListOf<HomeShelfItem>()

        for (i in 0 until contents.length()) {
            val content = contents.optJSONObject(i) ?: continue

            val twoRow = content.optJSONObject(
                "musicTwoRowItemRenderer"
            )

            if (twoRow != null) {
                parseTwoRowItem(twoRow)?.let(items::add)
                continue
            }

            val multiRow = content.optJSONObject(
                "musicMultiRowListItemRenderer"
            )

            if (multiRow != null) {
                parsePodcastEpisode(multiRow)?.let(items::add)
                continue
            }

            val responsive = content.optJSONObject(
                "musicResponsiveListItemRenderer"
            )

            if (responsive != null) {
                parseResponsiveItem(responsive)?.let(items::add)
            }
        }

        if (items.isEmpty()) return null

        return HomeShelf(
            title = title,
            subtitle = subtitle,
            strapline = subtitle,
            moreBrowseId = moreBrowseId,
            items = items,
            sourceItemCount = contents.length()
        )
    }

    private fun parseTwoRowItem(
        item: JSONObject
    ): HomeShelfItem? {
        val navigation = item.optJSONObject("navigationEndpoint")
        val extras = parseHomeItemExtras(
            item,
            navigation,
            ::parseSong
        )

        val song = parseSong(item)

        if (song != null) {
            return HomeShelfItem.Track(
                track = song,
                aspectRatio = extras.aspectRatio,
                itemType = extras.itemType,
                caption = extras.caption,
                cardData = parseCardData(item)
            )
        }

        val browse = navigation?.optJSONObject("browseEndpoint")
            ?: item.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")

        val browseId = browse?.optString("browseId")
            ?.takeIf { !it.isNullOrBlank() && it != "null" }
            ?: return null

        if (browseId.startsWith("UC")) return null

        val title = readText(item.optJSONObject("title"))
            ?: return null

        val subtitle = item.optJSONObject("subtitle")
        val author = readText(subtitle)

        val artwork = thumbnailUrl(item)

        val pageType = parsePageType(navigation)

        if (
            browseId.startsWith("MPSP") ||
            pageType == "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE"
        ) {
            return HomeShelfItem.PodcastShow(
                browseId = browseId,
                title = title,
                author = author,
                artworkUrl = artwork,
                aspectRatio = extras.aspectRatio,
                caption = extras.caption,
                cardData = parseCardData(item)
            )
        }

        val playlistId = browseId.removePrefix("VL")

        return HomeShelfItem.Playlist(
            playlist = YouTubePlaylistSummary(
                id = playlistId,
                title = title,
                author = author,
                artworkUrl = artwork
            ),
            aspectRatio = extras.aspectRatio,
            itemType = extras.itemType ?: "playlist",
            caption = extras.caption,
            collageArtworkUrls = extras.collageArtworkUrls,
            previewSongs = extras.previewSongs,
            cardData = parseCardData(item)
        )
    }

    private fun parseResponsiveItem(
        item: JSONObject
    ): HomeShelfItem? {
        val song = parseSong(item)

        if (song != null) {
            return HomeShelfItem.Track(
                track = song
            )
        }

        val columns = item.optJSONArray("flexColumns")
            ?: return null

        val firstColumn = columns.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")

        val title = readText(firstColumn) ?: return null

        val navigation = firstColumn
            ?.optJSONArray("runs")
            ?.optJSONObject(0)
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("browseEndpoint")
            ?: item.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")

        val browseId = navigation?.optString("browseId")
            ?.takeIf { !it.isNullOrBlank() && it != "null" }
            ?: return null

        if (browseId.startsWith("UC")) return null

        return HomeShelfItem.Playlist(
            playlist = YouTubePlaylistSummary(
                id = browseId.removePrefix("VL"),
                title = title,
                artworkUrl = thumbnailUrl(item)
            ),
            itemType = "playlist"
        )
    }

    private fun parsePodcastEpisode(
        item: JSONObject
    ): HomeShelfItem.PodcastEpisode? {
        val videoId = findString(item, "videoId")
            ?: return null

        val title = readText(item.optJSONObject("title"))
            ?: return null

        val secondTitle = item.optJSONObject("secondTitle")
        val showName = readText(secondTitle)

        val showBrowseId = secondTitle
            ?.optJSONArray("runs")
            ?.let { runs ->
                findString(runs, "browseId")
            }

        val subtitle = readText(item.optJSONObject("subtitle"))

        val progress = item
            .optJSONObject("playbackProgress")
            ?.optJSONObject("musicPlaybackProgressRenderer")

        val rawProgress = progress
            ?.optString("playbackProgressPercentage")
            ?.toFloatOrNull()

        val fraction = rawProgress?.let {
            if (it > 1f) it / 100f else it
        }?.coerceIn(0f, 1f)

        return HomeShelfItem.PodcastEpisode(
            videoId = videoId,
            title = title,
            showName = showName,
            showBrowseId = showBrowseId,
            subtitle = subtitle,
            durationText = readText(
                progress?.optJSONObject("durationText")
            ),
            progressFraction = fraction,
            artworkUrl = thumbnailUrl(item)
        )
    }

    private fun parseSong(
        item: JSONObject
    ): YouTubeMusicTrack? {
        val videoId = findString(item, "videoId")
            ?: return null

        val columns = item.optJSONArray("flexColumns")

        val title = columns
            ?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.let(::readText)
            ?: readText(item.optJSONObject("title"))
            ?: return null

        val artist = columns
            ?.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.let(::readText)
            ?: readText(item.optJSONObject("subtitle"))
            ?: "Unknown Artist"

        return YouTubeMusicTrack(
            videoId = videoId,
            title = title,
            artist = artist,
            artworkUrl = thumbnailUrl(item)
        )
    }

    private fun parseCardData(
        item: JSONObject
    ): HomeCardData {
        val navigation = item.optJSONObject("navigationEndpoint")
        val browse = navigation?.optJSONObject("browseEndpoint")

        return HomeCardData(
            isTwoRow = true,
            browseId = browse?.optString("browseId")
                ?.takeIf { it.isNotBlank() && it != "null" },
            pageType = parsePageType(navigation),
            watchVideoId = findString(item, "videoId"),
            largeArtworkUrl = thumbnailUrl(item)
        )
    }

    private fun parseChips(
        root: JSONObject
    ): List<HomeChip> {
        val section = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")

        val chips = section
            ?.optJSONObject("header")
            ?.optJSONObject("chipCloudRenderer")
            ?.optJSONArray("chips")
            ?: return emptyList()

        return buildList {
            for (i in 0 until chips.length()) {
                val renderer = chips.optJSONObject(i)
                    ?.optJSONObject("chipCloudChipRenderer")
                    ?: continue

                val title = readText(
                    renderer.optJSONObject("text")
                ) ?: continue

                val browse = renderer
                    .optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?: continue

                val params = browse.optString("params")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: continue

                add(
                    HomeChip(
                        title = title,
                        params = params,
                        browseId = browse.optString("browseId")
                            .takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        }
    }

    private fun parseContinuation(
        root: JSONObject
    ): String? {
        val section = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")

        val continuations = section?.optJSONArray("continuations")

        if (continuations != null) {
            for (i in 0 until continuations.length()) {
                val token = continuations.optJSONObject(i)
                    ?.optJSONObject("nextContinuationData")
                    ?.optString("continuation")

                if (!token.isNullOrBlank() && token != "null") {
                    return token
                }
            }
        }

        return findString(root, "continuation")
    }

    private fun parseBackground(
        root: JSONObject
    ): String? {
        return thumbnailUrl(
            root.optJSONObject("background")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?: return null
        )
    }

    private fun readText(
        node: JSONObject?
    ): String? {
        if (node == null) return null

        val simple = node.optString("simpleText")
        if (simple.isNotBlank() && simple != "null") {
            return simple.trim()
        }

        val runs = node.optJSONArray("runs")
            ?: return null

        val text = buildString {
            for (i in 0 until runs.length()) {
                append(
                    runs.optJSONObject(i)
                        ?.optString("text")
                        .orEmpty()
                )
            }
        }.trim()

        return text.takeIf { it.isNotBlank() }
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

                        if (url.startsWith("http")) return url
                        if (url.startsWith("//")) return "https:$url"
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

    private fun findString(
        value: Any?,
        target: String = "videoId"
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
}
