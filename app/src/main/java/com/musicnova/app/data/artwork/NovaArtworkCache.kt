package com.musicnova.app.data.artwork

import android.content.Context
import org.json.JSONObject

class NovaArtworkCache(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(
            "nova_artwork_cache",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val KEY_ENTRIES = "artwork_entries"
        private const val MAX_ENTRIES = 300
        private const val CACHE_VALIDITY =
            30L * 24 * 60 * 60 * 1000
    }

    @Synchronized
    fun get(key: String): String? {
        val entries = readEntries()
        val entry = entries.optJSONObject(key)
            ?: return null

        val savedAt = entry.optLong("savedAt", 0L)
        val url = entry.optString("url")

        if (
            System.currentTimeMillis() - savedAt >
            CACHE_VALIDITY
        ) {
            entries.remove(key)
            saveEntries(entries)
            return null
        }

        return url.takeIf {
            ArtworkNormalizer.isRealImage(it)
        }
    }

    @Synchronized
    fun put(key: String, url: String) {
        if (!ArtworkNormalizer.isRealImage(url)) return

        val entries = readEntries()

        entries.put(
            key,
            JSONObject()
                .put("url", url)
                .put("savedAt", System.currentTimeMillis())
        )

        val keys = entries.keys().asSequence().toList()

        val sorted = keys.sortedBy {
            entries.optJSONObject(it)?.optLong("savedAt") ?: 0L
        }

        sorted.forEach { oldKey ->
            val savedAt = entries.optJSONObject(oldKey)
                ?.optLong("savedAt") ?: 0L

            if (
                System.currentTimeMillis() - savedAt >
                CACHE_VALIDITY
            ) {
                entries.remove(oldKey)
            }
        }

        while (entries.length() > MAX_ENTRIES) {
            val oldest = entries.keys().asSequence()
                .minByOrNull {
                    entries.optJSONObject(it)?.optLong("savedAt")
                        ?: 0L
                } ?: break

            entries.remove(oldest)
        }

        saveEntries(entries)
    }

    private fun readEntries(): JSONObject {
        return try {
            JSONObject(
                preferences.getString(KEY_ENTRIES, "{}") ?: "{}"
            )
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun saveEntries(entries: JSONObject) {
        preferences.edit()
            .putString(KEY_ENTRIES, entries.toString())
            .apply()
    }
}
