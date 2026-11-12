
package com.musicnova.app.playback

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Music Nova playback error handling foundation.
 *
 * Handles network retry with exponential backoff.
 * Queue skip and fresh stream resolution will be
 * connected through MusicPlayer in the next step.
 */
class PlaybackErrorHandler(
    private val player: Player,
    private val maxRetries: Int = 3,
    private val onGiveUp: (PlaybackException) -> Unit = {}
) : Player.Listener {

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var retryJob: Job? = null
    private var retryCount = 0
    private var released = false

    override fun onPlayerError(error: PlaybackException) {
        if (released) return

        retryJob?.cancel()

        retryCount++

        Log.w(
            TAG,
            "Playback error: ${error.errorCodeName}, attempt=$retryCount",
            error
        )

        val retryable = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> true

            else -> false
        }

        if (!retryable || retryCount > maxRetries) {
            retryCount = 0
            onGiveUp(error)
            return
        }

        val position = player.currentPosition.coerceAtLeast(0L)

        retryJob = scope.launch {
            val waitMs =
                1000L * (1L shl (retryCount - 1).coerceAtMost(4))

            delay(waitMs)

            if (released) return@launch

            player.seekTo(position)
            player.prepare()
            player.play()
        }
    }

    override fun onMediaItemTransition(
        mediaItem: androidx.media3.common.MediaItem?,
        reason: Int
    ) {
        retryJob?.cancel()
        retryCount = 0
    }

    fun release() {
        if (released) return

        released = true
        retryJob?.cancel()
        scope.cancel()
        player.removeListener(this)
    }

    private companion object {
        const val TAG = "NovaPlaybackError"
    }
}
