
package com.musicnova.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicnova.app.data.artwork.NovaArtworkRepository
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

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recommendations = data.recommendations,
                    trending = data.trending,
                    recentlyPlayed = data.recentlyPlayed,
                    likedCount = data.likedCount,
                    nativeHomePage = data.nativeHomePage,
                    error = null
                )

                val (recommendations, trending) =
                    resolveHomeArtwork(
                        data.recommendations,
                        data.trending
                    )

                val updatedNativeHomePage =
                    resolveQuickPickArtwork(data.nativeHomePage)

                _uiState.value = _uiState.value.copy(
                    recommendations = recommendations,
                    trending = trending,
                    nativeHomePage = updatedNativeHomePage
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

    private suspend fun resolveHomeArtwork(
        recommendations: List<YouTubeMusicTrack>,
        trending: List<YouTubeMusicTrack>
    ): Pair<List<YouTubeMusicTrack>, List<YouTubeMusicTrack>> =
        coroutineScope {

            val semaphore = Semaphore(4)

            val allTracks = (
                recommendations + trending
            ).distinctBy { it.videoId }

            val resolvedTracks = allTracks.map { track ->
                async {
                    if (
                        track.isArtist ||
                        track.title.isBlank()
                    ) {
                        return@async track
                    }

                    semaphore.withPermit {
                        try {
                            val artwork =
                                artworkRepository.resolve(
                                    title = track.title,
                                    artist = track.artist,
                                    youtubeFallback = track.artworkUrl
                                )

                            if (artwork.isNullOrBlank()) {
                                track
                            } else {
                                track.copy(artworkUrl = artwork)
                            }

                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            track
                        }
                    }
                }
            }.awaitAll()

            val artworkByVideoId =
                resolvedTracks.associateBy { it.videoId }

            val updatedRecommendations =
                recommendations.map { track ->
                    artworkByVideoId[track.videoId] ?: track
                }

            val updatedTrending =
                trending.map { track ->
                    artworkByVideoId[track.videoId] ?: track
                }

            updatedRecommendations to updatedTrending
        }

    private suspend fun resolveQuickPickArtwork(
        homePage: HomePage?
    ): HomePage? {
        if (homePage == null) return null

        return coroutineScope {
            val semaphore = Semaphore(4)

            val updatedShelves = homePage.shelves.map { shelf ->
                val isQuickPick = shelf.title
                    .contains("quick pick", ignoreCase = true)

                if (!isQuickPick) {
                    return@map shelf
                }

                val updatedItems = shelf.items.map { item ->
                    if (item !is HomeShelfItem.Track) {
                        return@map item
                    }

                    val track = item.track

                    if (
                        track.isArtist ||
                        track.title.isBlank()
                    ) {
                        return@map item
                    }

                    semaphore.withPermit {
                        try {
                            val artwork =
                                artworkRepository.resolve(
                                    title = track.title,
                                    artist = track.artist,
                                    youtubeFallback =
                                        item.cardData?.largeArtworkUrl
                                            ?: track.artworkUrl
                                )

                            if (artwork.isNullOrBlank()) {
                                item
                            } else {
                                item.copy(
                                    track = track.copy(
                                        artworkUrl = artwork
                                    ),
                                    cardData = item.cardData?.copy(
                                        largeArtworkUrl = artwork
                                    )
                                )
                            }

                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            item
                        }
                    }
                }

                shelf.copy(items = updatedItems)
            }

            homePage.copy(shelves = updatedShelves)
        }
    }

    private suspend fun resolveSearchArtwork(
        tracks: List<YouTubeMusicTrack>
    ): List<YouTubeMusicTrack> = coroutineScope {

        val semaphore = Semaphore(4)

        tracks.map { track ->
            async {
                if (
                    track.isArtist ||
                    track.title.isBlank()
                ) {
                    return@async track
                }

                semaphore.withPermit {
                    try {
                        val artwork =
                            artworkRepository.resolve(
                                title = track.title,
                                artist = track.artist,
                                youtubeFallback = track.artworkUrl
                            )

                        if (artwork.isNullOrBlank()) {
                            track
                        } else {
                            track.copy(artworkUrl = artwork)
                        }

                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        track
                    }
                }
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

                val resolvedResults =
                    resolveSearchArtwork(results)

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

                    _uiState.value = _uiState.value.copy(
                        recommendations = data.recommendations,
                        trending = data.trending,
                        recentlyPlayed = data.recentlyPlayed,
                        likedCount = data.likedCount,
                        nativeHomePage = data.nativeHomePage,
                        error = null
                    )

                    val (recommendations, trending) =
                        resolveHomeArtwork(
                            data.recommendations,
                            data.trending
                        )

                    val updatedNativeHomePage =
                        resolveQuickPickArtwork(data.nativeHomePage)

                    _uiState.value = _uiState.value.copy(
                        recommendations = recommendations,
                        trending = trending,
                        nativeHomePage = updatedNativeHomePage
                    )

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message
                            ?: "Unable to refresh music"
                    )
                }
            }

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message
                    ?: "Unable to update liked songs"
            )
        }
    }

    fun refresh() {
        loadHomeMusic()
    }
}
