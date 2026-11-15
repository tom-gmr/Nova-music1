
package com.musicnova.app.data.music

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class HomeMusicData(
    val recommendations: List<YouTubeMusicTrack>,
    val trending: List<YouTubeMusicTrack>,
    val recentlyPlayed: List<LibraryTrack>,
    val likedCount: Int,
    val nativeHomePage: HomePage? = null
)

class MusicRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val libraryStore = LibraryStore(context)
    private val musicApi = InnerTubeMusicApi()

    suspend fun loadHomeMusic(
        currentTrack: YouTubeMusicTrack? = null
    ): HomeMusicData {

        val recent = try {
            libraryStore.getRecentlyPlayed()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MusicRepository", "Recently played loading failed", e)
            emptyList()
        }

        val liked = try {
            libraryStore.getLikedSongs()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MusicRepository", "Liked songs loading failed", e)
            emptyList()
        }

        val nativeHomePage = try {
            musicApi.fetchHomePage()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MusicRepository", "Native home feed failed", e)
            null
        }

        // Current playing song gets first priority as a recommendation seed.
        val currentSeed = currentTrack
            ?.takeIf { it.videoId.isNotBlank() && !it.isArtist }
            ?.let {
                LibraryTrack(
                    videoId = it.videoId,
                    title = it.title,
                    artist = it.artist,
                    artworkUrl = it.artworkUrl
                )
            }

        val seeds = (
            listOfNotNull(currentSeed) + liked + recent
        )
            .distinctBy { it.videoId }
            .take(8)

        val found = mutableListOf<YouTubeMusicTrack>()

        if (seeds.isEmpty()) {
            found.addAll(
                searchSafely(
                    query = "popular trending songs India",
                    limit = 30,
                    section = "Recommendations"
                )
            )
        } else {
            seeds.forEach { song ->
                found.addAll(
                    searchSafely(
                        query = "songs like ${song.title} by ${song.artist}",
                        limit = 10,
                        section = "Recommendations"
                    )
                )
            }
        }

        // Avoid recommending songs already present in recent/liked history,
        // and avoid recommending the song that is currently playing.
        val excluded = (
            liked.map { it.videoId } +
                recent.map { it.videoId } +
                listOfNotNull(currentSeed?.videoId)
            ).toSet()

        val playbackStats = try {
            libraryStore.readPlaybackStats()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MusicRepository", "Playback stats loading failed", e)
            emptyMap()
        }

        val recommendationSignals = RecommendationSignals(
            likedVideoIds = liked
                .map { it.videoId }
                .toSet(),

            recentlyPlayedVideoIds = recent
                .map { it.videoId }
                .toSet(),

            excludedVideoIds = excluded,

            listenedDurationMs = playbackStats.mapValues {
                (_, stats) -> stats.listenedDurationMs
            },

            skipCounts = playbackStats.mapValues {
                (_, stats) -> stats.skipCount
            }
        )

        val recommendations = RecommendationEngine.rank(
            candidates = found,
            signals = recommendationSignals,
            limit = 15
        )

        val trending = searchSafely(
            query = "trending hit songs India",
            limit = 20,
            section = "Trending"
        )
            .filterNot {
                it.isArtist || it.videoId.isBlank()
            }
            .distinctBy { it.videoId }
            .take(12)

        return HomeMusicData(
            recommendations = recommendations,
            trending = trending,
            recentlyPlayed = recent,
            likedCount = liked.size,
            nativeHomePage = nativeHomePage
        )
    }

    private suspend fun searchSafely(
        query: String,
        limit: Int,
        section: String
    ): List<YouTubeMusicTrack> {
        return try {
            musicApi.searchSongs(query, limit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(
                "MusicRepository",
                "$section loading failed: $query",
                e
            )
            emptyList()
        }
    }

    suspend fun searchSongs(
        query: String
    ): List<YouTubeMusicTrack> {
        return musicApi.searchSongs(query)
    }

    suspend fun fetchBrowsePage(
        browseId: String
    ): HomePage {
        return musicApi.fetchBrowsePage(browseId)
    }

    suspend fun fetchPlaylistPreview(
        playlistId: String
    ): PlaylistPreview? {
        return musicApi.fetchPlaylistPreview(playlistId)
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
