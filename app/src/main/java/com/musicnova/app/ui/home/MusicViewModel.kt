package com.musicnova.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.musicnova.app.data.artwork.NovaArtworkRepository
import com.musicnova.app.data.music.HomePage
import com.musicnova.app.data.music.HomeShelfItem
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.MusicRepository
import com.musicnova.app.data.music.YouTubeMusicTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val artworkRepository: NovaArtworkRepository,
    @ApplicationContext private val context: Context
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

                val recommendations = resolveTrackList(data.recommendations)
                val trending = resolveTrackList(data.trending)
                val recentlyPlayed = resolveRecentList(data.recentlyPlayed)
                val nativePage = resolveNativePage(data.nativeHomePage)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recommendations = recommendations,
                        trending = trending,
                        recentlyPlayed = recentlyPlayed,
                        likedCount = data.likedCount,
                        nativeHomePage = nativePage,
                        error = null
                    )
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

    private suspend fun resolveTrackList(
        tracks: List<YouTubeMusicTrack>
    ): List<YouTubeMusicTrack> = coroutineScope {
        tracks.map { track ->
            async {
                if (track.isArtist || track.title.isBlank()) {
                    track
                } else {
                    val artwork = resolveAndPreload(
                        track.title,
                        track.artist,
                        track.artworkUrl
                    )
                    track.copy(artworkUrl = artwork)
                }
            }
        }.awaitAll()
    }

    private suspend fun resolveRecentList(
        tracks: List<LibraryTrack>
    ): List<LibraryTrack> = coroutineScope {
        tracks.map { track ->
            async {
                val artwork = resolveAndPreload(
                    track.title,
                    track.artist,
                    track.artworkUrl
                )
                track.copy(artworkUrl = artwork)
            }
        }.awaitAll()
    }

    private suspend fun resolveNativePage(
        page: HomePage?
    ): HomePage? {
        if (page == null) return null

        return coroutineScope {
            page.copy(
                shelves = page.shelves.map { shelf ->
                    val resolvedItems = shelf.items.map { item ->
                        async {
                            if (item !is HomeShelfItem.Track) {
                                item
                            } else {
                                val track = item.track
                                val fallback = item.cardData?.largeArtworkUrl
                                    ?: track.artworkUrl

                                val artwork = resolveAndPreload(
                                    track.title,
                                    track.artist,
                                    fallback
                                )

                                item.copy(
                                    track = track.copy(
                                        artworkUrl = artwork
                                    ),
                                    cardData = item.cardData?.copy(
                                        largeArtworkUrl = artwork
                                    )
                                )
                            }
                        }
                    }.awaitAll()

                    shelf.copy(items = resolvedItems)
                }
            )
        }
    }

    private suspend fun resolveAndPreload(
        title: String,
        artist: String,
        fallback: String?
    ): String? {
        if (title.isBlank()) return null

        return artworkSemaphore.withPermit {
            try {
                val artwork = artworkRepository.resolve(
                    title = title,
                    artist = artist,
                    youtubeFallback = fallback
                ) ?: return@withPermit null

                if (preloadImage(artwork)) artwork else null

            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun preloadImage(url: String): Boolean {
        if (url.isBlank()) return false

        val request = ImageRequest.Builder(context)
            .data(url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .build()

        return try {
            context.imageLoader.execute(request) is SuccessResult
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
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
                val readyResults = resolveTrackList(results)

                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = readyResults,
                        searchError = null
                    )
                }

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
