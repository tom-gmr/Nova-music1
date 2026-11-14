
package com.musicnova.app.playback

import com.musicnova.app.data.music.YouTubeMusicTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlaybackQueueState(
    val tracks: List<YouTubeMusicTrack> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleOrder: List<Int> = emptyList()
) {
    val currentTrack: YouTubeMusicTrack?
        get() = tracks.getOrNull(currentIndex)

    private val playbackOrder: List<Int>
        get() = if (shuffleEnabled) {
            shuffleOrder
        } else {
            tracks.indices.toList()
        }

    private val currentPosition: Int
        get() = playbackOrder.indexOf(currentIndex)

    val nextTrack: YouTubeMusicTrack?
        get() {
            if (currentIndex !in tracks.indices) return null

            if (repeatMode == RepeatMode.ONE) {
                return currentTrack
            }

            val nextPosition = currentPosition + 1

            if (nextPosition in playbackOrder.indices) {
                return tracks.getOrNull(playbackOrder[nextPosition])
            }

            return if (repeatMode == RepeatMode.ALL) {
                tracks.getOrNull(playbackOrder.firstOrNull() ?: -1)
            } else {
                null
            }
        }

    val previousTrack: YouTubeMusicTrack?
        get() {
            if (currentIndex !in tracks.indices) return null

            val previousPosition = currentPosition - 1

            if (previousPosition in playbackOrder.indices) {
                return tracks.getOrNull(playbackOrder[previousPosition])
            }

            return if (repeatMode == RepeatMode.ALL) {
                tracks.getOrNull(playbackOrder.lastOrNull() ?: -1)
            } else {
                null
            }
        }
}

class PlaybackQueue {

    private val lock = Any()

    private val _state =
        MutableStateFlow(PlaybackQueueState())

    val state: StateFlow<PlaybackQueueState> =
        _state.asStateFlow()

    private fun validTrack(track: YouTubeMusicTrack): Boolean {
        return !track.isArtist && track.videoId.isNotBlank()
    }

    private fun createShuffleOrder(
        size: Int,
        currentIndex: Int
    ): List<Int> {
        if (size <= 0) return emptyList()

        val remaining = (0 until size)
            .filter { it != currentIndex }
            .shuffled(Random.Default)

        return if (currentIndex in 0 until size) {
            listOf(currentIndex) + remaining
        } else {
            remaining
        }
    }

    fun setQueue(
        tracks: List<YouTubeMusicTrack>,
        startVideoId: String
    ) {
        synchronized(lock) {
            val validTracks = tracks
                .filter { validTrack(it) }
                .distinctBy { it.videoId }

            val index = validTracks.indexOfFirst {
                it.videoId == startVideoId
            }

            val currentIndex = if (index >= 0) index else {
                if (validTracks.isNotEmpty()) 0 else -1
            }

            val current = _state.value

            _state.value = PlaybackQueueState(
                tracks = validTracks,
                currentIndex = currentIndex,
                shuffleEnabled = current.shuffleEnabled,
                repeatMode = current.repeatMode,
                shuffleOrder = if (current.shuffleEnabled) {
                    createShuffleOrder(validTracks.size, currentIndex)
                } else {
                    validTracks.indices.toList()
                }
            )
        }
    }

    fun append(tracks: List<YouTubeMusicTrack>) {
        synchronized(lock) {
            val current = _state.value
            val existingIds = current.tracks
                .map { it.videoId }
                .toMutableSet()

            val newTracks = tracks.filter {
                validTrack(it) && existingIds.add(it.videoId)
            }

            if (newTracks.isEmpty()) return

            val updatedTracks = current.tracks + newTracks

            val updatedShuffleOrder = if (current.shuffleEnabled) {
                current.shuffleOrder +
                    (current.tracks.size until updatedTracks.size)
                        .shuffled(Random.Default)
            } else {
                updatedTracks.indices.toList()
            }

            _state.value = current.copy(
                tracks = updatedTracks,
                shuffleOrder = updatedShuffleOrder
            )
        }
    }

    fun addNext(track: YouTubeMusicTrack) {
        if (!validTrack(track)) return

        synchronized(lock) {
            val current = _state.value

            val oldCurrentTrack = current.currentTrack
            val updated = current.tracks
                .filterNot { it.videoId == track.videoId }
                .toMutableList()

            val currentIndex = if (oldCurrentTrack == null) {
                -1
            } else {
                updated.indexOfFirst {
                    it.videoId == oldCurrentTrack.videoId
                }
            }

            val insertIndex = if (currentIndex < 0) {
                0
            } else {
                currentIndex + 1
            }

            updated.add(insertIndex, track)

            val newCurrentIndex = if (currentIndex < 0) {
                0
            } else {
                currentIndex
            }

            val newShuffleOrder = if (current.shuffleEnabled) {
                createShuffleOrder(updated.size, newCurrentIndex)
            } else {
                updated.indices.toList()
            }

            _state.value = current.copy(
                tracks = updated,
                currentIndex = newCurrentIndex,
                shuffleOrder = newShuffleOrder
            )
        }
    }

    fun next(): YouTubeMusicTrack? {
        synchronized(lock) {
            val current = _state.value

            if (current.currentIndex !in current.tracks.indices) {
                return null
            }

            if (current.repeatMode == RepeatMode.ONE) {
                return current.currentTrack
            }

            val order = if (current.shuffleEnabled) {
                current.shuffleOrder
            } else {
                current.tracks.indices.toList()
            }

            val position = order.indexOf(current.currentIndex)
            if (position < 0) return null

            var nextPosition = position + 1

            if (nextPosition >= order.size) {
                if (current.repeatMode != RepeatMode.ALL) {
                    return null
                }
                nextPosition = 0
            }

            val nextIndex = order[nextPosition]
            val track = current.tracks.getOrNull(nextIndex) ?: return null

            _state.value = current.copy(
                currentIndex = nextIndex
            )

            return track
        }
    }

    fun previous(): YouTubeMusicTrack? {
        synchronized(lock) {
            val current = _state.value

            if (current.currentIndex !in current.tracks.indices) {
                return null
            }

            val order = if (current.shuffleEnabled) {
                current.shuffleOrder
            } else {
                current.tracks.indices.toList()
            }

            val position = order.indexOf(current.currentIndex)
            if (position < 0) return null

            var previousPosition = position - 1

            if (previousPosition < 0) {
                if (current.repeatMode != RepeatMode.ALL) {
                    return null
                }
                previousPosition = order.lastIndex
            }

            val previousIndex = order[previousPosition]
            val track = current.tracks.getOrNull(previousIndex) ?: return null

            _state.value = current.copy(
                currentIndex = previousIndex
            )

            return track
        }
    }

    fun peekNext(): YouTubeMusicTrack? {
        return _state.value.nextTrack
    }

    fun toggleShuffle(): Boolean {
        synchronized(lock) {
            val current = _state.value
            val enabled = !current.shuffleEnabled

            val order = if (enabled) {
                createShuffleOrder(
                    current.tracks.size,
                    current.currentIndex
                )
            } else {
                current.tracks.indices.toList()
            }

            _state.value = current.copy(
                shuffleEnabled = enabled,
                shuffleOrder = order
            )

            return enabled
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        synchronized(lock) {
            val current = _state.value

            if (current.shuffleEnabled == enabled) return

            val order = if (enabled) {
                createShuffleOrder(
                    current.tracks.size,
                    current.currentIndex
                )
            } else {
                current.tracks.indices.toList()
            }

            _state.value = current.copy(
                shuffleEnabled = enabled,
                shuffleOrder = order
            )
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        synchronized(lock) {
            _state.value = _state.value.copy(
                repeatMode = mode
            )
        }
    }

    fun cycleRepeatMode(): RepeatMode {
        synchronized(lock) {
            val nextMode = when (_state.value.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }

            _state.value = _state.value.copy(
                repeatMode = nextMode
            )

            return nextMode
        }
    }

    fun removeAt(index: Int): YouTubeMusicTrack? {
        synchronized(lock) {
            val current = _state.value

            if (index !in current.tracks.indices) return null

            val removedTrack = current.tracks[index]
            val updatedTracks = current.tracks.toMutableList()
            updatedTracks.removeAt(index)

            val newCurrentIndex = when {
                updatedTracks.isEmpty() -> -1
                index < current.currentIndex -> current.currentIndex - 1
                index == current.currentIndex ->
                    current.currentIndex.coerceAtMost(updatedTracks.lastIndex)
                else -> current.currentIndex
            }

            val order = if (current.shuffleEnabled) {
                current.shuffleOrder
                    .filter { it != index }
                    .map { if (it > index) it - 1 else it }
            } else {
                updatedTracks.indices.toList()
            }

            _state.value = current.copy(
                tracks = updatedTracks,
                currentIndex = newCurrentIndex,
                shuffleOrder = order
            )

            return removedTrack
        }
    }

    fun remove(videoId: String): YouTubeMusicTrack? {
        val index = _state.value.tracks.indexOfFirst {
            it.videoId == videoId
        }

        return if (index >= 0) removeAt(index) else null
    }

    fun move(fromIndex: Int, toIndex: Int): Boolean {
        synchronized(lock) {
            val current = _state.value

            if (fromIndex !in current.tracks.indices ||
                toIndex !in current.tracks.indices
            ) {
                return false
            }

            if (fromIndex == toIndex) return true

            val updatedTracks = current.tracks.toMutableList()
            val movedTrack = updatedTracks.removeAt(fromIndex)
            updatedTracks.add(toIndex, movedTrack)

            fun remap(index: Int): Int {
                return when {
                    index == fromIndex -> toIndex
                    fromIndex < toIndex &&
                        index in (fromIndex + 1)..toIndex -> index - 1
                    fromIndex > toIndex &&
                        index in toIndex until fromIndex -> index + 1
                    else -> index
                }
            }

            val updatedCurrentIndex = if (current.currentIndex >= 0) {
                remap(current.currentIndex)
            } else {
                -1
            }

            val updatedShuffleOrder = if (current.shuffleEnabled) {
                current.shuffleOrder.map { remap(it) }
            } else {
                updatedTracks.indices.toList()
            }

            _state.value = current.copy(
                tracks = updatedTracks,
                currentIndex = updatedCurrentIndex,
                shuffleOrder = updatedShuffleOrder
            )

            return true
        }
    }

    fun clear() {
        synchronized(lock) {
            val current = _state.value

            _state.value = PlaybackQueueState(
                shuffleEnabled = current.shuffleEnabled,
                repeatMode = current.repeatMode
            )
        }
    }
}
