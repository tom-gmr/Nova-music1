
package com.musicnova.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicnova.app.data.artwork.NovaArtworkRepository
import com.musicnova.app.data.music.HomeMusicData
import com.musicnova.app.data.music.HomePage
import com.musicnova.app.data.music.HomeShelfItem
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.MusicRepository
import com.musicnova.app.data.music.YouTubeMusicTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class MusicUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val recommendations: List<YouTubeMusicTrack> = emptyList(),
    val trending: List<YouTubeMusicTrack> = emptyList(),
    val recentlyPlayed: List<LibraryTrack> = emptyList(),
    val likedCount: Int = 0,
    val nativeHomePage: HomePage? = null,
    val searchResults: List<YouTubeMusicTrack> = emptyList(),
    val searchError: String? = null,
    val error: String? = null
)

private data class HomeArtworkBundle(
    val recommendations: List<YouTubeMusicTrack>,
    val trending: List<YouTubeMusicTrack>,
    val recentlyPlayed: List<LibraryTrack>,
    val nativeHomePage: HomePage?
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val artworkRepository: NovaArtworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())

    val uiState: StateFlow<MusicUiState> =
        _uiState.asStateFlow()

    fun loadHomeMusic() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val data = repository.loadHomeMusic()
                val artworkData = resolveHomeArtworkBundle(data)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recommendations = artworkData?.recommendations
                        ?: data.recommendations.map {
                            it.copy(artworkUrl = null)
                        },
                    trending = artworkData?.trending
                        ?: data.trending.map {
                            it.copy(artworkUrl = null)
                        },
                    recentlyPlayed = artworkData?.recentlyPlayed
                        ?: data.recentlyPlayed.map {
                            it.copy(artworkUrl = null)
                        },
                    likedCount = data.likedCount,
                    nativeHomePage = artworkData?.nativeHomePage
                        ?: clearNativeTrackArtwork(data.nativeHomePage),
                    error = null
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unable to load music"
                )
            }
        }
    }

    private suspend fun resolveHomeArtworkBundle(
        data: HomeMusicData
    ): HomeArtworkBundle? = withTimeoutOrNull(2500L) {

        coroutineScope {
            val recommendationsJob = async {
                resolveHomeArtwork(
                    data.recommendations
                )
            }

            val trendingJob = async {
                resolveHomeArtwork(
                    data.trending
                )
            }

            val recentJob = async {
                resolveRecentArtwork(
                    data.recentlyPlayed
                )
            }

            val nativeJob = async {
                resolveNativeArtwork(
                    data.nativeHomePage
                )
            }

            HomeArtworkBundle(
                recommendations = recommendationsJob.await(),
                trending = trendingJob.await(),
                recentlyPlayed = recentJob.await(),
                nativeHomePage = nativeJob.await()
            )
        }
    }

    private suspend fun resolveHomeArtwork(
        tracks: List<YouTubeMusicTrack>
    ): List<YouTubeMusicTrack> = coroutineScope {

        val semaphore = Semaphore(16)

        tracks.map { track ->
            async {
                if (track.isArtist || track.title.isBlank()) {
                    return@async track.copy(artworkUrl = null)
                }

                val artwork = semaphore.withPermit {
                    resolveArtwork(track.title, track.artist)
                }

                track.copy(artworkUrl = artwork)
            }
        }.awaitAll()
    }

    private suspend fun resolveRecentArtwork(
        tracks: List<LibraryTrack>
    ): List<LibraryTrack> = coroutineScope {

        val semaphore = Semaphore(16)

        tracks.map { track ->
            async {
                val artwork = semaphore.withPermit {
                    resolveArtwork(track.title, track.artist)
                }

                track.copy(artworkUrl = artwork)
            }
        }.awaitAll()
    }

    private suspend fun resolveNativeArtwork(
        homePage: HomePage?
    ): HomePage? {
        if (homePage == null) return null

        return coroutineScope {
            val semaphore = Semaphore(16)

            val updatedShelves = homePage.shelves.map { shelf ->
                val updatedItems = shelf.items.map { item ->

                    if (item !is HomeShelfItem.Track) {
                        return@map item
                    }

                    val track = item.track

                    val artwork = if (
                        track.isArtist || track.title.isBlank()
                    ) {
                        null
                    } else {
                        semaphore.withPermit {
                            resolveArtwork(
                                track.title,
                                track.artist
                            )
                        }
                    }

                    item.copy(
                        track = track.copy(
                            artworkUrl = artwork
                        ),
                        cardData = item.cardData?.copy(
                            largeArtworkUrl = artwork
                        )
                    )
                }

                shelf.copy(items = updatedItems)
            }

            homePage.copy(shelves = updatedShelves)
        }
    }

    private suspend fun resolveArtwork(
        title: String,
        artist: String
    ): String? {
        if (title.isBlank()) return null

        return try {
            artworkRepository.resolve(
                title = title,
                artist = artist,
                youtubeFallback = null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun clearNativeTrackArtwork(
        homePage: HomePage?
    ): HomePage? {
        if (homePage == null) return null

        return homePage.copy(
            shelves = homePage.shelves.map { shelf ->
                shelf.copy(
                    items = shelf.items.map { item ->
                        if (item is HomeShelfItem.Track) {
                            item.copy(
                                track = item.track.copy(
                                    artworkUrl = null
                                ),
                                cardData = item.cardData?.copy(
                                    largeArtworkUrl = null
                                )
                            )
                        } else {
                            item
                        }
                    }
                )
            }
        )
    }

    private suspend fun resolveSearchArtwork(
        tracks: List<YouTubeMusicTrack>
    ): List<YouTubeMusicTrack> = coroutineScope {

        val semaphore = Semaphore(8)

        tracks.map { track ->
            async {
                if (track.isArtist || track.title.isBlank()) {
                    return@async track.copy(artworkUrl = null)
                }

                val artwork = semaphore.withPermit {
                    resolveArtwork(track.title, track.artist)
                }

                track.copy(artworkUrl = artwork)
            }
        }.awaitAll()
    }

    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                searchError = null,
                isSearching = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                searchError = null,
                searchResults = emptyList()
            )

            try {
                val results = repository.searchSongs(query.trim())
                val resolvedResults = resolveSearchArtwork(results)

                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = resolvedResults,
                    searchError = null
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = e.message
                        ?: "Search failed. Try again."
                )
            }
        }
    }

    fun toggleLike(track: YouTubeMusicTrack) {
        try {
            repository.toggleLike(track)

            viewModelScope.launch {
                try {
                    val data = repository.loadHomeMusic()
                    val artworkData = resolveHomeArtworkBundle(data)

                    _uiState.value = _uiState.value.copy(
                        recommendations = artworkData?.recommendations
                            ?: data.recommendations.map {
                                it.copy(artworkUrl = null)
                            },
                        trending = artworkData?.trending
                            ?: data.trending.map {
                                it.copy(artworkUrl = null)
                            },
                        recentlyPlayed = artworkData?.recentlyPlayed
                            ?: data.recentlyPlayed.map {
                                it.copy(artworkUrl = null)
                            },
                        likedCount = data.likedCount,
                        nativeHomePage = artworkData?.nativeHomePage
                            ?: clearNativeTrackArtwork(data.nativeHomePage),
                        error = null
                    )

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Unable to refresh music"
                    )
                }
            }

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Unable to update liked songs"
            )
        }
    }

    fun refresh() {
        loadHomeMusic()
    }
}
