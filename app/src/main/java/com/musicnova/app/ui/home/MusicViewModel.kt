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

    private val artworkSemaphore = Semaphore(6)

    fun loadHomeMusic() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            try {
                val data = repository.loadHomeMusic()

                // Publish music data immediately.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recommendations = data.recommendations,
                        trending = data.trending,
                        recentlyPlayed = data.recentlyPlayed,
                        likedCount = data.likedCount,
                        nativeHomePage = data.nativeHomePage,
                        error = null
                    )
                }

                // Resolve artwork independently in background.
                scheduleArtworkResolution(
                    data.recommendations +
                        data.trending +
                        (data.nativeHomePage?.shelves
                            ?.flatMap { shelf ->
                                shelf.items.flatMap { item ->
                                    when (item) {
                                        is HomeShelfItem.Track ->
                                            listOf(item.track)

                                        is HomeShelfItem.Playlist ->
                                            item.previewSongs

                                        else -> emptyList()
                                    }
                                }
                            } ?: emptyList())
                )

                data.recentlyPlayed.forEach { track ->
                    resolveRecentArtwork(track)
                }

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

    private fun scheduleArtworkResolution(
        tracks: List<YouTubeMusicTrack>
    ) {
        tracks
            .filter { !it.isArtist && it.title.isNotBlank() }
            .distinctBy { it.videoId.ifBlank { it.title + it.artist } }
            .forEach { track ->
                viewModelScope.launch {
                    resolveTrackArtwork(track)
                }
            }
    }

    private suspend fun resolveTrackArtwork(
        track: YouTubeMusicTrack
    ) {
        artworkSemaphore.withPermit {
            try {
                val artwork = artworkRepository.resolve(
                    title = track.title,
                    artist = track.artist,
                    youtubeFallback = track.artworkUrl
                ) ?: return

                updateTrackArtwork(track.videoId, artwork)

            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the existing artwork if lookup fails.
            }
        }
    }

    private fun resolveRecentArtwork(track: LibraryTrack) {
        viewModelScope.launch {
            artworkSemaphore.withPermit {
                try {
                    val artwork = artworkRepository.resolve(
                        title = track.title,
                        artist = track.artist,
                        youtubeFallback = track.artworkUrl
                    ) ?: return@withPermit

                    _uiState.update { state ->
                        state.copy(
                            recentlyPlayed = state.recentlyPlayed.map {
                                if (it.videoId == track.videoId) {
                                    it.copy(artworkUrl = artwork)
                                } else it
                            }
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Preserve current cover.
                }
            }
        }
    }

    private fun updateTrackArtwork(
        videoId: String,
        artwork: String
    ) {
        _uiState.update { state ->

            val recommendations = state.recommendations.map {
                if (it.videoId == videoId) {
                    it.copy(artworkUrl = artwork)
                } else it
            }

            val trending = state.trending.map {
                if (it.videoId == videoId) {
                    it.copy(artworkUrl = artwork)
                } else it
            }

            val searchResults = state.searchResults.map {
                if (it.videoId == videoId) {
                    it.copy(artworkUrl = artwork)
                } else it
            }

            val nativePage = state.nativeHomePage?.let { page ->
                page.copy(
                    shelves = page.shelves.map { shelf ->
                        shelf.copy(
                            items = shelf.items.map { item ->
                                when (item) {
                                    is HomeShelfItem.Track -> {
                                        if (item.track.videoId == videoId) {
                                            item.copy(
                                                track = item.track.copy(
                                                    artworkUrl = artwork
                                                ),
                                                cardData = item.cardData?.copy(
                                                    largeArtworkUrl = artwork
                                                )
                                            )
                                        } else item
                                    }

                                    is HomeShelfItem.Playlist -> {
                                        item.copy(
                                            previewSongs = item.previewSongs.map {
                                                if (it.videoId == videoId) {
                                                    it.copy(artworkUrl = artwork)
                                                } else it
                                            }
                                        )
                                    }

                                    else -> item
                                }
                            }
                        )
                    }
                )
            }

            state.copy(
                recommendations = recommendations,
                trending = trending,
                searchResults = searchResults,
                nativeHomePage = nativePage
            )
        }
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

                // Show search results without waiting for artwork.
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = results,
                        searchError = null
                    )
                }

                scheduleArtworkResolution(results)

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

