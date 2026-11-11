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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val artworkSemaphore = Semaphore(8)

    fun loadHomeMusic() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            try {
                val data = repository.loadHomeMusic()

                // Render Home immediately. Do not wait for remote artwork.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recommendations = data.recommendations.map {
                            track -> track.copy(artworkUrl = null)
                        },
                        trending = data.trending.map {
                            track -> track.copy(artworkUrl = null)
                        },
                        recentlyPlayed = data.recentlyPlayed.map {
                            track -> track.copy(artworkUrl = null)
                        },
                        likedCount = data.likedCount,
                        nativeHomePage = clearNativeArtwork(data.nativeHomePage),
                        error = null
                    )
                }

                // Resolve artwork independently in the background.
                resolveTrackList(data.recommendations, "recommendations")
                resolveTrackList(data.trending, "trending")
                resolveRecentList(data.recentlyPlayed)
                resolveNativePage(data.nativeHomePage)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unable to load music"
                    )
                }
            }
        }
    }

    private fun resolveTrackList(
        tracks: List<YouTubeMusicTrack>,
        section: String
    ) {
        tracks.forEach { track ->
            if (track.isArtist || track.title.isBlank()) return@forEach

            viewModelScope.launch {
                val artwork = artworkSemaphore.withPermit {
                    resolveArtwork(track.title, track.artist)
                }

                if (artwork.isNullOrBlank()) return@launch

                _uiState.update { state ->
                    val updated = when (section) {
                        "recommendations" -> state.recommendations.map {
                            if (it.videoId == track.videoId)
                                it.copy(artworkUrl = artwork)
                            else it
                        }

                        "trending" -> state.trending.map {
                            if (it.videoId == track.videoId)
                                it.copy(artworkUrl = artwork)
                            else it
                        }

                        else -> state.searchResults.map {
                            if (it.videoId == track.videoId)
                                it.copy(artworkUrl = artwork)
                            else it
                        }
                    }

                    when (section) {
                        "recommendations" ->
                            state.copy(recommendations = updated)
                        "trending" ->
                            state.copy(trending = updated)
                        else ->
                            state.copy(searchResults = updated)
                    }
                }
            }
        }
    }

    private fun resolveRecentList(tracks: List<LibraryTrack>) {
        tracks.forEach { track ->
            viewModelScope.launch {
                val artwork = artworkSemaphore.withPermit {
                    resolveArtwork(track.title, track.artist)
                }

                if (artwork.isNullOrBlank()) return@launch

                _uiState.update { state ->
                    state.copy(
                        recentlyPlayed = state.recentlyPlayed.map {
                            if (it.videoId == track.videoId)
                                it.copy(artworkUrl = artwork)
                            else it
                        }
                    )
                }
            }
        }
    }

    private fun resolveNativePage(homePage: HomePage?) {
        if (homePage == null) return

        homePage.shelves.forEach { shelf ->
            shelf.items.forEach { item ->
                if (item !is HomeShelfItem.Track) return@forEach

                val track = item.track
                if (track.isArtist || track.title.isBlank()) return@forEach

                viewModelScope.launch {
                    val artwork = artworkSemaphore.withPermit {
                        resolveArtwork(track.title, track.artist)
                    }

                    if (artwork.isNullOrBlank()) return@launch

                    _uiState.update { state ->
                        val currentPage = state.nativeHomePage ?: return@update state

                        state.copy(
                            nativeHomePage = currentPage.copy(
                                shelves = currentPage.shelves.map { currentShelf ->
                                    currentShelf.copy(
                                        items = currentShelf.items.map { currentItem ->
                                            if (
                                                currentItem is HomeShelfItem.Track &&
                                                currentItem.track.videoId == track.videoId
                                            ) {
                                                currentItem.copy(
                                                    track = currentItem.track.copy(
                                                        artworkUrl = artwork
                                                    ),
                                                    cardData = currentItem.cardData?.copy(
                                                        largeArtworkUrl = artwork
                                                    )
                                                )
                                            } else {
                                                currentItem
                                            }
                                        }
                                    )
                                }
                            )
                        )
                    }
                }
            }
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

    private fun clearNativeArtwork(homePage: HomePage?): HomePage? {
        if (homePage == null) return null

        return homePage.copy(
            shelves = homePage.shelves.map { shelf ->
                shelf.copy(
                    items = shelf.items.map { item ->
                        if (item is HomeShelfItem.Track) {
                            item.copy(
                                track = item.track.copy(artworkUrl = null),
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

    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchError = null,
                    isSearching = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    searchError = null,
                    searchResults = emptyList()
                )
            }

            try {
                val results = repository.searchSongs(query.trim())

                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = results.map { track ->
                            track.copy(artworkUrl = null)
                        },
                        searchError = null
                    )
                }

                resolveTrackList(results, "search")

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = e.message ?: "Search failed. Try again."
                    )
                }
            }
        }
    }

    fun toggleLike(track: YouTubeMusicTrack) {
        try {
            repository.toggleLike(track)
            loadHomeMusic()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = e.message ?: "Unable to update liked songs")
            }
        }
    }

    fun refresh() {
        loadHomeMusic()
    }
}
