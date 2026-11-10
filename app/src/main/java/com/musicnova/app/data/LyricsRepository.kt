package com.musicnova.app.data.lyrics

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TrackLyrics(
    val syncedLyrics: String?,
    val plainLyrics: String?
) {
    val hasSyncedLyrics: Boolean
        get() = !syncedLyrics.isNullOrBlank()

    val displayText: String?
        get() = syncedLyrics?.takeIf { it.isNotBlank() }
            ?: plainLyrics?.takeIf { it.isNotBlank() }
}

class LyricsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun findLyrics(
        artist: String,
        track: String
    ): TrackLyrics? = withContext(Dispatchers.IO) {

        if (artist.isBlank() || track.isBlank()) {
            return@withContext null
        }

        // First try exact match
        val exactUrl = Uri.parse("https://lrclib.net/api/get")
            .buildUpon()
            .appendQueryParameter("artist_name", artist)
            .appendQueryParameter("track_name", track)
            .build()
            .toString()

        val exactResult = requestRecord(exactUrl)

        if (exactResult != null) {
            return@withContext exactResult
        }

        // Fallback: search matching tracks
        val searchUrl = Uri.parse("https://lrclib.net/api/search")
            .buildUpon()
            .appendQueryParameter("track_name", track)
            .appendQueryParameter("artist_name", artist)
            .build()
            .toString()

        return@withContext searchRecords(
            searchUrl,
            artist,
            track
        )
    }

    private fun requestRecord(url: String): TrackLyrics? {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "MusicNova/1.0 (https://github.com/tom-gmr/Nova-music1)"
            )
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use null
                }

                val body = response.body?.string()
                    ?: return@use null

                val json = JSONObject(body)
                readLyrics(json)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun searchRecords(
        url: String,
        artist: String,
        track: String
    ): TrackLyrics? {

        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "MusicNova/1.0 (https://github.com/tom-gmr/Nova-music1)"
            )
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use null
                }

                val body = response.body?.string()
                    ?: return@use null

                val results = org.json.JSONArray(body)
                if (results.length() == 0) {
                    return@use null
                }

                val normalizedArtist = normalize(artist)
                val normalizedTrack = normalize(track)

                val candidates = (0 until results.length())
                    .mapNotNull { index ->
                        results.optJSONObject(index)
                    }
                    .sortedByDescending { item ->
                        val resultArtist = normalize(
                            item.optString("artistName")
                        )
                        val resultTrack = normalize(
                            item.optString("trackName")
                        )

                        (if (resultArtist == normalizedArtist) 4 else 0) +
                        (if (resultTrack == normalizedTrack) 4 else 0) +
                        (if (resultArtist.contains(normalizedArtist) ||
                            normalizedArtist.contains(resultArtist)) 1 else 0)
                    }

                for (item in candidates) {
                    val result = readLyrics(item)
                    if (result != null) {
                        return@use result
                    }
                }

                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readLyrics(json: JSONObject): TrackLyrics? {
        val synced = json.optString("syncedLyrics")
            .takeIf { it.isNotBlank() && it != "null" }

        val plain = json.optString("plainLyrics")
            .takeIf { it.isNotBlank() && it != "null" }

        return if (synced == null && plain == null) {
            null
        } else {
            TrackLyrics(
                syncedLyrics = synced,
                plainLyrics = plain
            )
        }
    }

    private fun normalize(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }
}
