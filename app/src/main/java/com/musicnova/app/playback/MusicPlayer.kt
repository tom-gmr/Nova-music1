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
    private var errorHandler: PlaybackErrorHandler? = null
    private var released = false
    private var pendingPlay: PendingPlay? = null
    private var pendingQueue: PendingQueue? = null

    private lateinit var controllerFuture:
        ListenableFuture<MediaController>

    private val _playbackState =
        MutableStateFlow(PlaybackState())

    val playbackState: StateFlow<PlaybackState> =
        _playbackState.asStateFlow()

    @Volatile
    private var playbackErrorListener:
        ((PlaybackException) -> Unit)? = null

    @Volatile
    private var trackEndedListener: (() -> Unit)? = null

    private data class PendingPlay(
        val streamUrl: String,
        val title: String,
        val artist: String,
        val artworkUrl: String?,
        val requestHeaders: Map<String, String>
    )

    private data class PendingQueue(
        val items: List<MediaItem>,
        val startIndex: Int
    )

    fun setOnPlaybackErrorListener(
        listener: ((PlaybackException) -> Unit)?
    ) {
        playbackErrorListener = listener
    }

    fun setOnTrackEndedListener(
        listener: (() -> Unit)?
    ) {
        trackEndedListener = listener
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

                val currentController = controller

                if (
                    playbackState == Player.STATE_ENDED &&
                    (currentController?.mediaItemCount ?: 0) <= 1
                ) {
                    trackEndedListener?.invoke()
                }
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

                    errorHandler = PlaybackErrorHandler(
                        player = connectedController,
                        maxRetries = 3,
                        onGiveUp = { error ->
                            playbackErrorListener?.invoke(error)
                        }
                    )

                    updatePlaybackState()

                    pendingQueue?.let { queue ->
                        pendingQueue = null
                        startQueue(
                            connectedController,
                            queue.items,
                            queue.startIndex
                        )
                    } ?: pendingPlay?.let { request ->
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

        pendingQueue = null

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

        pendingPlay = null

        startPlayback(
            connectedController,
            request
        )
    }

    private fun buildMediaItem(
        request: PendingPlay
    ): MediaItem {
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

        return MediaItem.Builder()
            .setMediaId(request.streamUrl)
            .setUri(Uri.parse(request.streamUrl))
            .setMediaMetadata(metadata)
            .setCustomCacheKey(request.streamUrl)
            .build()
    }

    private fun startPlayback(
        player: MediaController,
        request: PendingPlay
    ) {
        val mediaItem = buildMediaItem(request)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        updatePlaybackState()
    }

    // Media3 playlist support

    fun setQueue(
        items: List<MediaItem>,
        startIndex: Int = 0
    ) {
        if (items.isEmpty()) {
            clearQueue()
            return
        }

        val safeIndex = startIndex.coerceIn(
            0,
            items.lastIndex
        )

        pendingPlay = null

        val connectedController = controller

        if (connectedController == null) {
            pendingQueue = PendingQueue(
                items = items.toList(),
                startIndex = safeIndex
            )
            return
        }

        pendingQueue = null

        startQueue(
            connectedController,
            items,
            safeIndex
        )
    }

    private fun startQueue(
        player: MediaController,
        items: List<MediaItem>,
        startIndex: Int
    ) {
        if (items.isEmpty()) return

        val safeIndex = startIndex.coerceIn(
            0,
            items.lastIndex
        )

        player.setMediaItems(
            items,
            safeIndex,
            0L
        )

        player.prepare()
        player.play()

        updatePlaybackState()
    }

    fun addQueueItems(
        items: List<MediaItem>
    ) {
        if (items.isEmpty()) return

        controller?.addMediaItems(items)
    }

    fun removeQueueItem(index: Int): Boolean {
        val player = controller ?: return false

        if (index !in 0 until player.mediaItemCount) {
            return false
        }

        player.removeMediaItem(index)
        return true
    }

    fun moveQueueItem(
        fromIndex: Int,
        toIndex: Int
    ): Boolean {
        val player = controller ?: return false

        if (
            fromIndex !in 0 until player.mediaItemCount ||
            toIndex !in 0 until player.mediaItemCount
        ) {
            return false
        }

        player.moveMediaItem(
            fromIndex,
            toIndex
        )

        return true
    }

    fun clearQueue() {
        pendingQueue = null
        pendingPlay = null

        controller?.run {
            stop()
            clearMediaItems()
        }

        _playbackState.value = PlaybackState()
    }

    fun setShuffleEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun isShuffleEnabled(): Boolean {
        return controller?.shuffleModeEnabled == true
    }

    fun setRepeatMode(mode: RepeatMode) {
        val playerMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }

        controller?.repeatMode = playerMode
    }

    fun getRepeatMode(): RepeatMode {
        return when (controller?.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
    }

    fun playNextMediaItem() {
        controller?.seekToNextMediaItem()
    }

    fun playPreviousMediaItem() {
        controller?.seekToPreviousMediaItem()
    }

    fun getMediaItemCount(): Int {
        return controller?.mediaItemCount ?: 0
    }

    fun getCurrentMediaItemIndex(): Int {
        return controller?.currentMediaItemIndex ?: -1
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

    // Full Player progress support

    fun getCurrentPosition(): Long {
        return controller?.currentPosition
            ?.coerceAtLeast(0L) ?: 0L
    }

    fun getDuration(): Long {
        return controller?.duration
            ?.takeIf { it > 0L } ?: 0L
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
        val currentController = controller ?: return

        if (currentController.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun stop() {
        clearQueue()
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
        pendingQueue = null
        playbackErrorListener = null
        trackEndedListener = null

        errorHandler?.release()
        errorHandler = null

        controller?.run {
            removeListener(playerListener)
            release()
        }

        controller = null
    }
}
