package com.musicnova.app.data.artwork

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class NovaArtworkRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val persistentCache = NovaArtworkCache(context)

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val memoryCache =
        mutableMapOf<String, String>()

    suspend fun resolve(
        title: String,
        artist: String,
        youtubeFallback: String? = null
    ): String? = withContext(Dispatchers.IO) {

        if (title.isBlank()) {
            return@withContext youtubeFallback
        }

        val key = ArtworkNormalizer.cacheKey(title, artist)

        // 1. Check memory cache
        synchronized(memoryCache) {
            memoryCache[key]?.let {
                return@withContext it
            }
        }

        // 2. Check persistent cache
        persistentCache.get(key)?.let { cached ->
            synchronized(memoryCache) {
                memoryCache[key] = cached
            }
            return@withContext cached
        }

        // 3. Search Apple Music
        val apple = fetchAppleMusic(title, artist)

        if (!apple.isNullOrBlank()) {
            synchronized(memoryCache) {
                memoryCache[key] = apple
            }
            persistentCache.put(key, apple)

            return@withContext apple
        }

        // 4. Search Deezer
        val deezer = fetchDeezer(title, artist)

        if (!deezer.isNullOrBlank()) {
            synchronized(memoryCache) {
                memoryCache[key] = deezer
            }
            persistentCache.put(key, deezer)

            return@withContext deezer
        }

        // 5. YouTube fallback
        val fallback =
            ArtworkNormalizer.upscaleYoutubeArtwork(youtubeFallback)

        if (!fallback.isNullOrBlank()) {
            synchronized(memoryCache) {
                memoryCache[key] = fallback
            }
        }

        fallback
    }

    private fun fetchAppleMusic(
        title: String,
        artist: String
    ): String? {
        return runCatching {
            val query = URLEncoder.encode(
                "$title $artist",
                "UTF-8"
            )

            val request = Request.Builder()
                .url(
                    "https://itunes.apple.com/search?term=$query&entity=song&limit=10"
                )
                .build()

            val body = http.newCall(request)
                .execute()
                .use { response ->
                    if (!response.isSuccessful) return null
                    response.body?.string() ?: return null
                }

            val results = JSONObject(body)
                .optJSONArray("results") ?: return null

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue

                val candidateTitle =
                    item.optString("trackName")

                val candidateArtist =
                    item.optString("artistName")

                if (!matches(title, candidateTitle)) continue

                if (
                    artist.isNotBlank() &&
                    !matches(artist, candidateArtist)
                ) continue

                val image = item.optString("artworkUrl100")

                if (image.startsWith("https://")) {
                    return image.replace(
                        Regex("""\d+x\d+bb"""),
                        "1200x1200bb"
                    )
                }
            }

            null
        }.getOrNull()
    }

    private fun fetchDeezer(
        title: String,
        artist: String
    ): String? {
        return runCatching {
            val query = URLEncoder.encode(
                "$title $artist",
                "UTF-8"
            )

            val request = Request.Builder()
                .url("https://api.deezer.com/search?q=$query&limit=10")
                .build()

            val body = http.newCall(request)
                .execute()
                .use { response ->
                    if (!response.isSuccessful) return null
                    response.body?.string() ?: return null
                }

            val results = JSONObject(body)
                .optJSONArray("data") ?: return null

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue

                val candidateTitle =
                    item.optString("title")

                val candidateArtist =
                    item.optJSONObject("artist")
                        ?.optString("name")
                        .orEmpty()

                if (!matches(title, candidateTitle)) continue

                if (
                    artist.isNotBlank() &&
                    !matches(artist, candidateArtist)
                ) continue

                val album = item.optJSONObject("album")
                    ?: continue

                val image = album.optString("cover_xl")

                if (image.startsWith("https://")) {
                    return image
                }
            }

            null
        }.getOrNull()
    }

    private fun matches(
        first: String,
        second: String
    ): Boolean {
        fun clean(value: String) =
            value.lowercase()
                .replace(Regex("""\(.*?\)|\[.*?]"""), "")
                .replace(Regex("""[^a-z0-9]"""), "")

        val a = clean(first)
        val b = clean(second)

        return a.isNotBlank() &&
            b.isNotBlank() &&
            (a == b || a.startsWith(b) || b.startsWith(a))
    }
}
