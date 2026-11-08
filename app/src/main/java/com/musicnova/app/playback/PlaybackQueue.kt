
package com.musicnova.app.playback

import com.musicnova.app.data.music.YouTubeMusicTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackQueueState(
    val tracks: List<YouTubeMusicTrack> = emptyList(),
    val currentIndex: Int = -1
) {
    val currentTrack: YouTubeMusicTrack?
        get() = tracks.getOrNull(currentIndex)

    val nextTrack: YouTubeMusicTrack?
        get() = tracks.getOrNull(currentIndex + 1)

    val previousTrack: YouTubeMusicTrack?
        get() = tracks.getOrNull(currentIndex - 1)
}

class PlaybackQueue {

    private val lock = Any()

    private val _state =
        MutableStateFlow(PlaybackQueueState())

    val state: StateFlow<PlaybackQueueState> =
        _state.asStateFlow()

    fun setQueue(
        tracks: List<YouTubeMusicTrack>,
        startVideoId: String
    ) {
        synchronized(lock) {
            val validTracks = tracks
                .filter { !it.isArtist && it.videoId.isNotBlank() }
                .distinctBy { it.videoId }

            val index = validTracks.indexOfFirst {
                it.videoId == startVideoId
            }

            _state.value = PlaybackQueueState(
                tracks = validTracks,
                currentIndex = if (index >= 0) index else 0
            )
        }
    }

    fun append(
        tracks: List<YouTubeMusicTrack>
    ) {
        synchronized(lock) {
            val current = _state.value
            val existingIds = current.tracks
                .map { it.videoId }
                .toMutableSet()

            val newTracks = tracks.filter {
                !it.isArtist &&
                it.videoId.isNotBlank() &&
                existingIds.add(it.videoId)
            }

            _state.value = current.copy(
                tracks = current.tracks + newTracks
            )
        }
    }

    fun addNext(
        track: YouTubeMusicTrack
    ) {
        if (track.isArtist || track.videoId.isBlank()) return

        synchronized(lock) {
            val current = _state.value

            val updated = current.tracks
                .filterNot { it.videoId == track.videoId }
                .toMutableList()

            val insertIndex = if (current.currentIndex < 0) {
                0
            } else {
                (current.currentIndex + 1)
                    .coerceAtMost(updated.size)
            }

            updated.add(insertIndex, track)

            val newIndex = if (current.currentIndex < 0) {
                0
            } else if (insertIndex <= current.currentIndex) {
                current.currentIndex + 1
            } else {
                current.currentIndex
            }

            _state.value = PlaybackQueueState(
                tracks = updated,
                currentIndex = newIndex
            )
        }
    }

    fun next(): YouTubeMusicTrack? {
        synchronized(lock) {
            val current = _state.value
            val nextIndex = current.currentIndex + 1

            if (nextIndex !in current.tracks.indices) {
                return null
            }

            val track = current.tracks[nextIndex]

            _state.value = current.copy(
                currentIndex = nextIndex
            )

            return track
        }
    }

    fun previous(): YouTubeMusicTrack? {
        synchronized(lock) {
            val current = _state.value
            val previousIndex = current.currentIndex - 1

            if (previousIndex !in current.tracks.indices) {
                return null
            }

            val track = current.tracks[previousIndex]

            _state.value = current.copy(
                currentIndex = previousIndex
            )

            return track
        }
    }

    fun peekNext(): YouTubeMusicTrack? {
        return _state.value.nextTrack
    }

    fun clear() {
        synchronized(lock) {
            _state.value = PlaybackQueueState()
        }
    }
}
