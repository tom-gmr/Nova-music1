
package com.musicnova.app.data.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class YouTubeMusicTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val isArtist: Boolean = false
)

class InnerTubeMusicApi {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val musicUrl = "https://music.youtube.com"

    private data class WebConfig(
        val apiKey: String,
        val clientVersion: String
    )

    private suspend fun getWebConfig(): WebConfig =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$musicUrl/")
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("YouTube Music config failed: ${response.code}")
                }

                val html = response.body?.string()
                    ?: error("Empty YouTube Music response")

                val apiKey = Regex(
                    "\"INNERTUBE_API_KEY\"\\s*:\\s*\"([^\"]+)\""
                ).find(html)?.groupValues?.get(1)
                    ?: error("YouTube Music API key not found")

                val version = Regex(
                    "\"INNERTUBE_CLIENT_VERSION\"\\s*:\\s*\"([^\"]+)\""
                ).find(html)?.groupValues?.get(1)
                    ?: error("YouTube Music client version not found")

                WebConfig(apiKey, version)
            }
        }

    private fun createContext(config: WebConfig): JSONObject {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", config.clientVersion)
            .put("hl", "en")
            .put("gl", "IN")

        return JSONObject().put("client", client)
    }

    private fun browseRequest(
        config: WebConfig,
        browseId: String
    ): Request {
        val body = JSONObject()
            .put("context", createContext(config))
            .put("browseId", browseId)
            .toString()

        return Request.Builder()
            .url("$musicUrl/youtubei/v1/browse?key=${config.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .header("Origin", musicUrl)
            .header("Referer", "$musicUrl/")
            .header("Content-Type", "application/json")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", config.clientVersion)
            .build()
    }

    private fun executeJson(request: Request, errorLabel: String): JSONObject {
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("$errorLabel failed: ${response.code}")
            }

            JSONObject(
                response.body?.string()
                    ?: error("Empty $errorLabel response")
            )
        }
    }

    suspend fun searchSongs(
        query: String,
        limit: Int = 30
    ): List<YouTubeMusicTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val config = getWebConfig()
        val body = JSONObject()
            .put("context", createContext(config))
            .put("query", query.trim())
            .toString()

        val request = Request.Builder()
            .url("$musicUrl/youtubei/v1/search?key=${config.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .header("Origin", musicUrl)
            .header("Referer", "$musicUrl/")
            .header("Content-Type", "application/json")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", config.clientVersion)
            .build()

        val root = executeJson(request, "YouTube Music search")
        val results = mutableListOf<YouTubeMusicTrack>()
        val seen = mutableSetOf<String>()

        fun addResult(track: YouTubeMusicTrack?) {
            if (track == null) return

            val key = if (track.isArtist) {
                "artist:${track.title.lowercase()}"
            } else {
                "song:${track.videoId}"
            }

            if (seen.add(key)) results.add(track)
        }

        fun collect(value: Any?) {
            when (value) {
                is JSONObject -> {
                    value.optJSONObject("musicResponsiveListItemRenderer")
                        ?.let { renderer ->
                            val song = parseSong(renderer)
                            addResult(song ?: parseResponsiveArtist(renderer))
                        }

                    value.optJSONObject("musicTwoRowItemRenderer")
                        ?.let { addResult(parseArtist(it)) }

                    val keys = value.keys()
                    while (keys.hasNext()) {
                        collect(value.opt(keys.next()))
                    }
                }

                is org.json.JSONArray -> {
                    for (i in 0 until value.length()) {
                        collect(value.opt(i))
                    }
                }
            }
        }

        collect(root)
        results.take(limit.coerceIn(1, 100))
    }

    /** Fetches the native YouTube Music Home feed. */
    suspend fun fetchHomePage(): HomePage =
        withContext(Dispatchers.IO) {
            val config = getWebConfig()
            val request = browseRequest(config, "FEmusic_home")
            val root = executeJson(request, "YouTube Music Home")
            HomeFeedParser.parseHomePage(root)
        }

    /** Fetches a YouTube Music browse page using its browse ID. */
    suspend fun fetchBrowsePage(
        browseId: String
    ): HomePage = withContext(Dispatchers.IO) {
        val id = browseId.trim()
        require(id.isNotBlank()) {
            "Browse ID cannot be empty"
        }

        val config = getWebConfig()
        val request = browseRequest(config, id)
        val root = executeJson(request, "YouTube Music browse")
        if (id.startsWith("VL")) {
    PlaylistBrowseParser.parse(root)
} else {
    HomeFeedParser.parseHomePage(root)
        
    }

    /** Fetches the first page of a playlist for card previews. */
    suspend fun fetchPlaylistPreview(
        playlistId: String
    ): PlaylistPreview? = withContext(Dispatchers.IO) {
        val id = playlistId.trim().removePrefix("VL")
        if (id.isBlank()) return@withContext null

        val config = getWebConfig()
        val request = browseRequest(config, "VL$id")
        val root = executeJson(request, "Playlist preview")

        PlaylistPreviewParser.parse(root)
    }

    private fun parseSong(
        renderer: JSONObject
    ): YouTubeMusicTrack? {
        val videoId = findStringValue(renderer, "videoId")
            ?: return null

        val columns = renderer.optJSONArray("flexColumns")
            ?: return null

        if (columns.length() == 0) return null

        val title = textFromNode(columns.opt(0))
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val artist = if (columns.length() > 1) {
            textFromNode(columns.opt(1))
                ?.takeIf { it.isNotBlank() }
                ?: "Unknown Artist"
        } else {
            "Unknown Artist"
        }

        return YouTubeMusicTrack(
            videoId = videoId,
            title = title,
            artist = artist,
            artworkUrl = thumbnailUrl(renderer),
            isArtist = false
        )
    }

    private fun parseArtist(
        renderer: JSONObject
    ): YouTubeMusicTrack? {
        val title = textFromNode(renderer.opt("title"))
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val browseId = findStringValue(renderer, "browseId")
            ?: return null

        val subtitle = textFromNode(renderer.opt("subtitle")) ?: ""
        val isArtist = browseId.startsWith("UC") ||
            subtitle.contains("artist", ignoreCase = true)

        if (!isArtist) return null

        return YouTubeMusicTrack(
            videoId = "",
            title = title,
            artist = "Artist",
            artworkUrl = thumbnailUrl(renderer),
            isArtist = true
        )
    }

    private fun parseResponsiveArtist(
        renderer: JSONObject
    ): YouTubeMusicTrack? {
        val browseId = findStringValue(renderer, "browseId")
            ?: return null

        val columns = renderer.optJSONArray("flexColumns")
            ?: return null

        if (columns.length() == 0) return null

        val title = textFromNode(columns.opt(0))
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val subtitle = if (columns.length() > 1) {
            textFromNode(columns.opt(1)) ?: ""
        } else {
            ""
        }

        val isArtist = browseId.startsWith("UC") ||
            subtitle.contains("artist", ignoreCase = true)

        if (!isArtist) return null

        return YouTubeMusicTrack(
            videoId = "",
            title = title,
            artist = "Artist",
            artworkUrl = thumbnailUrl(renderer),
            isArtist = true
        )
    }

    private fun textFromNode(value: Any?): String? {
        when (value) {
            is String -> return value.takeIf { it.isNotBlank() }

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
                                runs.optJSONObject(i)
                                    ?.optString("text")
                                    .orEmpty()
                            )
                        }
                    }
                    if (combined.isNotBlank()) return combined
                }

                val textValue = textFromNode(value.opt("text"))
                if (!textValue.isNullOrBlank()) return textValue

                val keys = value.keys()
                while (keys.hasNext()) {
                    val result = textFromNode(value.opt(keys.next()))
                    if (!result.isNullOrBlank()) return result
                }
            }

            is org.json.JSONArray -> {
                for (i in 0 until value.length()) {
                    val result = textFromNode(value.opt(i))
                    if (!result.isNullOrBlank()) return result
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
                    var bestQuality = -1L

                    for (i in 0 until thumbnails.length()) {
                        val item = thumbnails.optJSONObject(i) ?: continue
                        val url = item.optString("url").orEmpty()

                        if (!url.startsWith("https://") &&
                            !url.startsWith("http://")
                        ) continue

                        val width = item.optLong("width", 0L)
                        val height = item.optLong("height", 0L)
                        val quality = width * height

                        if (quality > bestQuality) {
                            bestQuality = quality
                            bestUrl = url
                        }
                    }

                    if (!bestUrl.isNullOrBlank()) return bestUrl
                }

                val keys = value.keys()
                while (keys.hasNext()) {
                    val result = thumbnailUrl(value.opt(keys.next()))
                    if (!result.isNullOrBlank()) return result
                }
            }

            is org.json.JSONArray -> {
                for (i in 0 until value.length()) {
                    val result = thumbnailUrl(value.opt(i))
                    if (!result.isNullOrBlank()) return result
                }
            }
        }

        return null
    }

    private fun findStringValue(
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
                    val result = findStringValue(
                        value.opt(keys.next()),
                        target
                    )
                    if (!result.isNullOrBlank()) return result
                }
            }

            is org.json.JSONArray -> {
                for (i in 0 until value.length()) {
                    val result = findStringValue(value.opt(i), target)
                    if (!result.isNullOrBlank()) return result
                }
            }
        }

        return null
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Safari/537.36"
    }
}
