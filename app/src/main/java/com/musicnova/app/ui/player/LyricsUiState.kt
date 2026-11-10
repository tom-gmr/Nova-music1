
package com.musicnova.app.ui.player

import com.musicnova.app.data.lyrics.LyricsResult

sealed interface LyricsUiState {

    data object Idle : LyricsUiState

    data object Loading : LyricsUiState

    data class Success(
        val result: LyricsResult.Success
    ) : LyricsUiState

    data object Empty : LyricsUiState

    data class Error(
        val message: String
    ) : LyricsUiState
}
