
package com.musicnova.app.data.music

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RecommendationCache(context: Context) {

    private val preferences = context.getSharedPreferences(
        "music_nova_recommendation_cache",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SEED = "seed_key"
        private const val KEY_SAVED_AT = "saved_at"
        private const val KEY_TRACKS = "tracks"

        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    }

    fun read(seedKey: String): List<YouTubeMusicTrack> {
        if (seedKey.isBlank()) return emptyList()

        return try {
            val savedSeed = preferences.getString(KEY_SEED, null)
            val savedAt = preferences.getLong(KEY_SAVED_AT, 0L)

            if (savedSeed != seedKey) return emptyList()

            val age = System.currentTimeMillis() - savedAt

            if (age < 0L || age >= CACHE_TTL_MS) {
                return emptyList()
            }

            val array = JSONArray(
                preferences.getString(KEY_TRACKS, "[]") ?: "[]"
            )

            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index)
                    ?: return@mapNotNull null

                val videoId = item.optString("videoId")
                if (videoId.isBlank()) return@mapNotNull null

                YouTubeMusicTrack(
                    videoId = videoId,
                    title = item.optString("title"),
                    artist = item.optString("artist"),
                    artworkUrl = item.optString("artworkUrl")
                        .takeIf { it.isNotBlank() && it != "null" },
                    isArtist = false,
                    isOfficial = item.optBoolean("isOfficial", false)
                )
            }.distinctBy { it.videoId }

        } catch (_: Exception) {
            emptyList()
        }
    }

    fun write(
        seedKey: String,
        tracks: List<YouTubeMusicTrack>
    ) {
        if (seedKey.isBlank()) return

        val array = JSONArray()

        tracks
            .filter { !it.isArtist && it.videoId.isNotBlank() }
            .distinctBy { it.videoId }
            .forEach { track ->
                array.put(
                    JSONObject()
                        .put("videoId", track.videoId)
                        .put("title", track.title)
                        .put("artist", track.artist)
                        .put("artworkUrl", track.artworkUrl)
                        .put("isOfficial", track.isOfficial)
                )
            }

        preferences.edit()
            .putString(KEY_SEED, seedKey)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .putString(KEY_TRACKS, array.toString())
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
