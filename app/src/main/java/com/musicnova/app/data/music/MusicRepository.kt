package com.musicnova.app.data.music

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class HomeMusicData(
    val recommendations: List<YouTubeMusicTrack>,
    val trending: List<YouTubeMusicTrack>,
    val recentlyPlayed: List<LibraryTrack>,
    val likedCount: Int
)

class MusicRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val libraryStore = LibraryStore(context)
    private val musicApi = InnerTubeMusicApi()

    suspend fun loadHomeMusic(): HomeMusicData {
        val recent = libraryStore.getRecentlyPlayed()
        val liked = libraryStore.getLikedSongs()
        val seeds = (liked + recent).distinctBy { it.videoId }.take(5)

        val found = mutableListOf<YouTubeMusicTrack>()

        if (seeds.isEmpty()) {
            found.addAll(
                musicApi.searchSongs("popular trending songs India", 30)
            )
        } else {
            seeds.forEach { song ->
                found.addAll(
                    musicApi.searchSongs(
                        "songs like ${song.title} by ${song.artist}",
                        10
                    )
                )
            }
        }

        val excluded = (liked + recent).map { it.videoId }.toSet()

        val recommendations = found
            .filterNot {
                it.isArtist ||
                it.videoId.isBlank() ||
                it.videoId in excluded
            }
            .distinctBy { it.videoId }
            .take(15)

        val trending = musicApi.searchSongs(
            "trending hit songs India", 20
        )
            .filterNot { it.isArtist || it.videoId.isBlank() }
            .distinctBy { it.videoId }
            .take(12)

        return HomeMusicData(
            recommendations = recommendations,
            trending = trending,
            recentlyPlayed = recent,
            likedCount = liked.size
        )
    }

    suspend fun searchSongs(
    query: String
): List<YouTubeMusicTrack> {
    return musicApi.searchSongs(query)
    }

    fun toggleLike(track: YouTubeMusicTrack): Boolean {
        return libraryStore.toggleLike(
            LibraryTrack(
                videoId = track.videoId,
                title = track.title,
                artist = track.artist,
                artworkUrl = track.artworkUrl
            )
        )
    }
}
