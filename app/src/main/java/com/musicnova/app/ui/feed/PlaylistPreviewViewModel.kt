package com.musicnova.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicnova.app.data.music.MusicRepository
import com.musicnova.app.data.music.PlaylistPreviewLoader
import com.musicnova.app.data.music.PlaylistPreviewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PlaylistPreviewViewModel @Inject constructor(
    repository: MusicRepository
) : ViewModel() {

    private val loader = PlaylistPreviewLoader(
        scope = viewModelScope,
        fetch = repository::fetchPlaylistPreview
    )

    fun load(
        playlistId: String
    ): StateFlow<PlaylistPreviewState> {
        return loader.load(playlistId)
    }
}
