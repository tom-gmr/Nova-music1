package com.musicnova.app.data.lyrics

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
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
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun findLyrics(
        artist: String,
        track: String
    ): TrackLyrics? = withContext(Dispatchers.IO) {

        if (artist.isBlank() || track.isBlank()) {
            return@withContext null
        }

        val url = Uri.parse("https://lrclib.net/api/get")
            .buildUpon()
            .appendQueryParameter("artist_name", artist)
            .appendQueryParameter("track_name", track)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MusicNova/1.0 (Android)")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null
                }

                val body = response.body?.string()
                    ?: return@withContext null

                val json = JSONObject(body)

                val synced = json.optString("syncedLyrics")
                    .takeIf { it.isNotBlank() && it != "null" }

                val plain = json.optString("plainLyrics")
                    .takeIf { it.isNotBlank() && it != "null" }

                if (synced == null && plain == null) {
                    null
                } else {
                    TrackLyrics(synced, plain)
                }
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
