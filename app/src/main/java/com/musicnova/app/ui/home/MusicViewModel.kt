
package com.musicnova.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicnova.app.data.music.HomePage
import com.musicnova.app.data.music.LibraryTrack
import com.musicnova.app.data.music.MusicRepository
import com.musicnova.app.data.music.YouTubeMusicTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val repository: MusicRepository
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

                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = results,
                    searchError = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = e.message ?: "Search failed. Try again."
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
