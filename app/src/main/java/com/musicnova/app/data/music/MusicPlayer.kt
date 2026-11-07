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
    val artworkUrl: String?
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
                    error("YouTube Music config request failed: ${response.code}")
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

    suspend fun searchSongs(
        query: String,
        limit: Int = 30
    ): List<YouTubeMusicTrack> = withContext(Dispatchers.IO) {

        if (query.isBlank()) return@withContext emptyList()

        val config = getWebConfig()

        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", config.clientVersion)
            .put("hl", "en")
            .put("gl", "IN")

        val context = JSONObject()
            .put("client", client)

        val body = JSONObject()
            .put("context", context)
            .put("query", query.trim())
            .toString()

        val request = Request.Builder()
            .url("$musicUrl/youtubei/v1/search?key=${config.apiKey}")
            .post(
                body.toRequestBody(
                    "application/json".toMediaType()
                )
            )
            .header("User-Agent", USER_AGENT)
            .header("Origin", musicUrl)
            .header("Referer", "$musicUrl/")
            .header("Content-Type", "application/json")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", config.clientVersion)
            .build()

        val root = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("YouTube Music search failed: ${response.code}")
            }

            JSONObject(
                response.body?.string()
                    ?: error("Empty search response")
            )
        }

        val results = mutableListOf<YouTubeMusicTrack>()

        fun collect(value: Any?) {
            when (value) {
                is JSONObject -> {
                    val renderer = value.optJSONObject(
                        "musicResponsiveListItemRenderer"
                    )

                    if (renderer != null) {
                        parseTrack(renderer)?.let { track ->
                            if (results.none { it.videoId == track.videoId }) {
                                results.add(track)
                            }
                        }
                    }

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

    private fun parseTrack(
        renderer: JSONObject
    ): YouTubeMusicTrack? {

        val videoId =
            findString(renderer, "videoId")
                ?: return null

        val columns = renderer.optJSONArray("flexColumns")
            ?: return null

        if (columns.length() == 0) return null

        val titleColumn = columns.optJSONObject(0)
            ?: return null

        val title = findString(titleColumn, "text")
            ?: return null

        val artist = if (columns.length() > 1) {
            findString(columns.optJSONObject(1), "text")
                ?: "Unknown Artist"
        } else {
            "Unknown Artist"
        }

        val artwork = findString(renderer, "url")

        return YouTubeMusicTrack(
            videoId = videoId,
            title = title,
            artist = artist,
            artworkUrl = artwork
        )
    }

    private fun findString(
        obj: JSONObject?,
        key: String
    ): String? {

        if (obj == null) return null

        val direct = obj.optString(key)
        if (direct.isNotBlank() && direct != "null") {
            return direct
        }

        val keys = obj.keys()

        while (keys.hasNext()) {
            val value = obj.opt(keys.next())

            when (value) {
                is JSONObject -> {
                    val result = findString(value, key)
                    if (!result.isNullOrBlank()) return result
                }

                is org.json.JSONArray -> {
                    for (i in 0 until value.length()) {
                        val item = value.optJSONObject(i)
                        val result = findString(item, key)
                        if (!result.isNullOrBlank()) return result
                    }
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
