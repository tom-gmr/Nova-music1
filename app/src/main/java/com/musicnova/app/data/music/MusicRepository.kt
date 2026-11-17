package com.musicnova.app.data.music

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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
    private val recommendationCache = RecommendationCache(context)

    companion object {
        private const val MAX_SEEDS = 8
        private const val MAX_PARALLEL_REQUESTS = 4
        private const val RECOMMENDATION_LIMIT = 15
        private const val TRENDING_LIMIT = 12
    }
        suspend fun loadHomeMusic(
        currentTrack: YouTubeMusicTrack? = null,
        onCachedRecommendations: (List<YouTubeMusicTrack>) -> Unit = {}
    ): HomeMusicData = coroutineScope {

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

        val seeds = (listOfNotNull(currentSeed) + liked + recent)
            .distinctBy { it.videoId }
            .take(MAX_SEEDS)

        val cacheKey = currentSeed?.videoId ?: "discover"

        val excluded = (
            liked.map { it.videoId } +
                recent.map { it.videoId } +
                listOfNotNull(currentSeed?.videoId)
            ).toSet()

        // Cache-first: show saved recommendations immediately.
        val cachedRecommendations = recommendationCache.read(cacheKey)
            .filterNot {
                it.isArtist ||
                    it.videoId.isBlank() ||
                    it.videoId in excluded
            }
            .distinctBy { it.videoId }
            .take(RECOMMENDATION_LIMIT)

        if (cachedRecommendations.isNotEmpty()) {
            onCachedRecommendations(cachedRecommendations)
        }

        // Limit simultaneous YouTube Music requests.
        val requestSemaphore = Semaphore(MAX_PARALLEL_REQUESTS)

        // Load Home feed, recommendation candidates and Trending together.
                val homeJob = async {
            requestSemaphore.withPermit {
                try {
                    musicApi.fetchHomePage()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("MusicRepository", "Native home feed failed", e)
                    null
                }
            }
                }

        val recommendationJob = async {
            val queries = if (seeds.isEmpty()) {
                listOf("popular trending songs India")
            } else {
                seeds.map {
                    "songs like ${it.title} by ${it.artist}"
                }
            }

            queries.map { query ->
                async {
                    requestSemaphore.withPermit {
                        searchSafely(
                            query = query,
                            limit = 10,
                            section = "Recommendations"
                        )
                    }
                }
            }.awaitAll().flatten()
        }

        val trendingJob = async {
            requestSemaphore.withPermit {
                searchSafely(
                    query = "trending hit songs India",
                    limit = 20,
                    section = "Trending"
                )
            }
        }

        val nativeHomePage = homeJob.await()
        val found = recommendationJob.await()
        val trendingCandidates = trendingJob.await()

        val playbackStats = try {
            libraryStore.readPlaybackStats()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MusicRepository", "Playback stats loading failed", e)
            emptyMap()
        }

        val recommendationSignals = RecommendationSignals(
            likedVideoIds = liked.map { it.videoId }.toSet(),
            recentlyPlayedVideoIds = recent.map { it.videoId }.toSet(),
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
            limit = RECOMMENDATION_LIMIT
        )

        if (recommendations.isNotEmpty()) {
            recommendationCache.write(
                seedKey = cacheKey,
                tracks = recommendations
            )
        }

        val trending = trendingCandidates
            .filterNot {
                it.isArtist || it.videoId.isBlank()
            }
            .distinctBy { it.videoId }
            .take(TRENDING_LIMIT)

        HomeMusicData(
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

    fun toggleLike(
        track: YouTubeMusicTrack
    ): Boolean {
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
