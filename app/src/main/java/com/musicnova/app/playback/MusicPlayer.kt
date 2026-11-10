package com.musicnova.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val hasMedia: Boolean = false
)

class MusicPlayer(context: Context) {

    private val appContext = context.applicationContext

    private var controller: MediaController? = null
    private var released = false

    private var pendingPlay: PendingPlay? = null

    private lateinit var controllerFuture:
        ListenableFuture<MediaController>

    private val _playbackState =
        MutableStateFlow(PlaybackState())

    val playbackState: StateFlow<PlaybackState> =
        _playbackState.asStateFlow()

    @Volatile
    private var playbackErrorListener:
        ((PlaybackException) -> Unit)? = null

    private data class PendingPlay(
        val streamUrl: String,
        val title: String,
        val artist: String,
        val artworkUrl: String?,
        val requestHeaders: Map<String, String>
    )

    fun setOnPlaybackErrorListener(
        listener: ((PlaybackException) -> Unit)?
    ) {
        playbackErrorListener = listener
    }

    private val playerListener =
        object : Player.Listener {

            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {
                updatePlaybackState()
            }

            override fun onMediaMetadataChanged(
                mediaMetadata: MediaMetadata
            ) {
                updatePlaybackState()
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                updatePlaybackState()
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {
                updatePlaybackState()
            }

            override fun onPlayerError(
                error: PlaybackException
            ) {
                playbackErrorListener?.invoke(error)
            }
        }

    init {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(
                appContext,
                MusicPlaybackService::class.java
            )
        )

        controllerFuture =
            MediaController.Builder(
                appContext,
                sessionToken
            ).buildAsync()

        controllerFuture.addListener(
            {
                try {
                    val connectedController =
                        controllerFuture.get()

                    if (released) {
                        connectedController.release()
                        return@addListener
                    }

                    controller = connectedController

                    connectedController.addListener(
                        playerListener
                    )

                    updatePlaybackState()

                    pendingPlay?.let { request ->
                        pendingPlay = null
                        startPlayback(
                            connectedController,
                            request
                        )
                    }

                } catch (error: Exception) {
                    android.util.Log.e(
                        "MusicNova",
                        "MediaController connection failed",
                        error
                    )
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun play(
        streamUrl: String,
        title: String,
        artist: String,
        artworkUrl: String? = null,
        requestHeaders: Map<String, String> = emptyMap()
    ) {
        require(
            streamUrl.startsWith("https://") ||
                streamUrl.startsWith("http://")
        ) {
            "Valid HTTP(S) audio URL required"
        }

        val request = PendingPlay(
            streamUrl = streamUrl,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            requestHeaders = requestHeaders
        )

        _playbackState.value = PlaybackState(
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            isPlaying = false,
            hasMedia = true
        )

        val connectedController = controller

        if (connectedController == null) {
            pendingPlay = request
            return
        }

        startPlayback(
            connectedController,
            request
        )
    }

    private fun startPlayback(
        player: MediaController,
        request: PendingPlay
    ) {
        val headers = request.requestHeaders

        val extras = Bundle().apply {
            putStringArrayList(
                "nova_header_names",
                ArrayList(headers.keys)
            )

            putStringArrayList(
                "nova_header_values",
                ArrayList(headers.values)
            )
        }

        val metadata =
            MediaMetadata.Builder()
                .setTitle(request.title)
                .setArtist(request.artist)
                .apply {
                    if (!request.artworkUrl.isNullOrBlank()) {
                        setArtworkUri(
                            Uri.parse(request.artworkUrl)
                        )
                    }
                }
                .setExtras(extras)
                .build()

        val mediaItem =
            MediaItem.Builder()
                .setMediaId(request.streamUrl)
                .setUri(Uri.parse(request.streamUrl))
                .setMediaMetadata(metadata)
                .setCustomCacheKey(request.streamUrl)
                .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        updatePlaybackState()
    }

    private fun updatePlaybackState() {
        val currentController = controller ?: return

        val item = currentController.currentMediaItem
        val metadata = item?.mediaMetadata

        _playbackState.value = PlaybackState(
            title = metadata?.title?.toString().orEmpty(),
            artist = metadata?.artist?.toString().orEmpty(),
            artworkUrl = metadata?.artworkUri?.toString(),
            isPlaying = currentController.isPlaying,
            hasMedia = item != null
        )
    }

    fun pause() {
        controller?.pause()
        updatePlaybackState()
    }

    fun resume() {
        if (_playbackState.value.hasMedia) {
            controller?.play()
            updatePlaybackState()
        }
    }

    fun togglePlayPause() {
        val currentController = controller

        if (currentController == null) {
            return
        }

        if (currentController.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun stop() {
        pendingPlay = null

        controller?.run {
            stop()
            clearMediaItems()
        }

        _playbackState.value = PlaybackState()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(
            positionMs.coerceAtLeast(0L)
        )
    }

    fun isPlaying(): Boolean {
        return controller?.isPlaying == true
    }

    fun release() {
        if (released) return

        released = true
        pendingPlay = null
        playbackErrorListener = null

        controller?.run {
            removeListener(playerListener)
            release()
        }

        controller = null
    }
}
