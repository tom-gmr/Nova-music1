package com.musicnova.app.data.music

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class LibraryTrack(
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?
)

data class MusicPlaylist(
    val name: String,
    val tracks: List<LibraryTrack>
)

class LibraryStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        "music_nova_library",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_LIKED = "liked_songs"
        private const val KEY_RECENT = "recently_played"
        private const val KEY_PLAYLISTS = "playlists"
        private const val MAX_RECENT = 30

        @Volatile
        private var cloudReadyUid: String? = null
    }

    private val cloudScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Restores the signed-in user's library.
     * If no cloud backup exists, uploads the current local library.
     */
    suspend fun syncWithCloud(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return false

        cloudReadyUid = null

        val document = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("library")
            .document("main")

        val snapshot = document.get().await()

        if (auth.currentUser?.uid != uid) return false

        val localLiked =
            preferences.getString(KEY_LIKED, "[]") ?: "[]"

        val localRecent =
            preferences.getString(KEY_RECENT, "[]") ?: "[]"

        val localPlaylists =
            preferences.getString(KEY_PLAYLISTS, "[]") ?: "[]"

        if (snapshot.exists()) {

            val liked =
                snapshot.getString(KEY_LIKED) ?: localLiked

            val recent =
                snapshot.getString(KEY_RECENT) ?: localRecent

            val playlists =
                snapshot.getString(KEY_PLAYLISTS) ?: localPlaylists

            preferences.edit()
                .putString(KEY_LIKED, liked)
                .putString(KEY_RECENT, recent)
                .putString(KEY_PLAYLISTS, playlists)
                .commit()

        } else {

            document.set(
                mapOf(
                    KEY_LIKED to localLiked,
                    KEY_RECENT to localRecent,
                    KEY_PLAYLISTS to localPlaylists
                )
            ).await()
        }

        if (auth.currentUser?.uid != uid) return false

        cloudReadyUid = uid
        return true
    }

    private fun persistCloudField(
        key: String,
        value: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return

        if (cloudReadyUid != uid) return

        cloudScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .collection("library")
                    .document("main")
                    .set(
                        mapOf(key to value),
                        SetOptions.merge()
                    )
                    .await()

            } catch (e: Exception) {
                Log.w(
                    "LibraryStore",
                    "Cloud backup failed for $key",
                    e
                )
            }
        }
    }

    private fun trackToJson(
        track: LibraryTrack
    ): JSONObject {
        return JSONObject()
            .put("videoId", track.videoId)
            .put("title", track.title)
            .put("artist", track.artist)
            .put(
                "artworkUrl",
                track.artworkUrl ?: JSONObject.NULL
            )
    }

    private fun jsonToTrack(
        json: JSONObject
    ): LibraryTrack {
        val artwork = json.optString("artworkUrl", "")

        return LibraryTrack(
            videoId = json.optString("videoId"),
            title = json.optString("title"),
            artist = json.optString(
                "artist",
                "Unknown Artist"
            ),
            artworkUrl = artwork.takeIf {
                it.isNotBlank() && it != "null"
            }
        )
    }

    private fun saveTracks(
        key: String,
        tracks: List<LibraryTrack>
    ) {
        val array = JSONArray()

        tracks.forEach {
            array.put(trackToJson(it))
        }

        val json = array.toString()

        preferences.edit()
            .putString(key, json)
            .apply()

        persistCloudField(key, json)
    }

    private fun readTracks(
        key: String
    ): List<LibraryTrack> {
        return try {
            val array = JSONArray(
                preferences.getString(key, "[]") ?: "[]"
            )

            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index)
                    ?: return@mapNotNull null

                jsonToTrack(item)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getLikedSongs(): List<LibraryTrack> {
        return readTracks(KEY_LIKED)
    }

    fun isLiked(videoId: String): Boolean {
        return getLikedSongs().any {
            it.videoId == videoId
        }
    }

    fun toggleLike(track: LibraryTrack): Boolean {
        if (track.videoId.isBlank()) return false

        val songs = getLikedSongs().toMutableList()

        val existing = songs.any {
            it.videoId == track.videoId
        }

        if (existing) {
            songs.removeAll {
                it.videoId == track.videoId
            }
        } else {
            songs.add(0, track)
        }

        saveTracks(KEY_LIKED, songs)
        return !existing
    }

    fun removeLikedSong(videoId: String) {
        saveTracks(
            KEY_LIKED,
            getLikedSongs().filterNot {
                it.videoId == videoId
            }
        )
    }

    fun addRecentlyPlayed(track: LibraryTrack) {
        if (track.videoId.isBlank()) return

        val songs = getRecentlyPlayed()
            .filterNot {
                it.videoId == track.videoId
            }
            .toMutableList()

        songs.add(0, track)

        saveTracks(
            KEY_RECENT,
            songs.take(MAX_RECENT)
        )
    }

    fun getRecentlyPlayed(): List<LibraryTrack> {
        return readTracks(KEY_RECENT)
    }

    fun getPlaylists(): List<MusicPlaylist> {
        return try {
            val array = JSONArray(
                preferences.getString(KEY_PLAYLISTS, "[]") ?: "[]"
            )

            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index)
                    ?: return@mapNotNull null

                val name = item.optString("name")

                if (name.isBlank()) {
                    return@mapNotNull null
                }

                val tracksArray =
                    item.optJSONArray("tracks") ?: JSONArray()

                val tracks =
                    (0 until tracksArray.length()).mapNotNull {
                        tracksArray.optJSONObject(it)?.let(::jsonToTrack)
                    }

                MusicPlaylist(name, tracks)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePlaylists(
        playlists: List<MusicPlaylist>
    ) {
        val array = JSONArray()

        playlists.forEach { playlist ->
            val tracks = JSONArray()

            playlist.tracks.forEach {
                tracks.put(trackToJson(it))
            }

            array.put(
                JSONObject()
                    .put("name", playlist.name)
                    .put("tracks", tracks)
            )
        }

        val json = array.toString()

        preferences.edit()
            .putString(KEY_PLAYLISTS, json)
            .apply()

        persistCloudField(KEY_PLAYLISTS, json)
    }

    fun createPlaylist(name: String): Boolean {
        val cleanName = name.trim()

        if (cleanName.isBlank()) return false

        val playlists = getPlaylists().toMutableList()

        if (playlists.any {
                it.name.equals(
                    cleanName,
                    ignoreCase = true
                )
            }) {
            return false
        }

        playlists.add(
            MusicPlaylist(cleanName, emptyList())
        )

        savePlaylists(playlists)
        return true
    }

    fun addToPlaylist(
        playlistName: String,
        track: LibraryTrack
    ): Boolean {
        if (track.videoId.isBlank()) return false

        val playlists = getPlaylists().toMutableList()

        val index = playlists.indexOfFirst {
            it.name == playlistName
        }

        if (index == -1) return false

        val playlist = playlists[index]

        if (playlist.tracks.any {
                it.videoId == track.videoId
            }) {
            return false
        }

        playlists[index] = playlist.copy(
            tracks = listOf(track) + playlist.tracks
        )

        savePlaylists(playlists)
        return true
    }

    fun removeFromPlaylist(
        playlistName: String,
        videoId: String
    ) {
        val playlists = getPlaylists().map { playlist ->
            if (playlist.name == playlistName) {
                playlist.copy(
                    tracks = playlist.tracks.filterNot {
                        it.videoId == videoId
                    }
                )
            } else {
                playlist
            }
        }

        savePlaylists(playlists)
    }

    fun deletePlaylist(name: String) {
        savePlaylists(
            getPlaylists().filterNot {
                it.name == name
            }
        )
    }
}
