
package com.musicnova.app.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.media3.common.PlaybackException
data class PlaybackState(
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val hasMedia: Boolean = false
)

class MusicPlayer(context: Context) {

    private val appContext = context.applicationContext

    private val httpDataSourceFactory =
        DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

    private val player: ExoPlayer =
        ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(httpDataSourceFactory)
            )
            .build()

    private val _playbackState =
        MutableStateFlow(PlaybackState())

    val playbackState: StateFlow<PlaybackState> =
        _playbackState.asStateFlow()
@Volatile
private var playbackErrorListener:
    ((PlaybackException) -> Unit)? = null

fun setOnPlaybackErrorListener(
    listener: ((PlaybackException) -> Unit)?
) {
    playbackErrorListener = listener
}
    val exoPlayer: ExoPlayer
        get() = player
init {
    player.addListener(object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value =
                _playbackState.value.copy(
                    isPlaying = isPlaying
                )
        }

        override fun onPlayerError(error: PlaybackException) {
            playbackErrorListener?.invoke(error)
        }
    })
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

        httpDataSourceFactory.setDefaultRequestProperties(
            requestHeaders
        )

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .apply {
                if (!artworkUrl.isNullOrBlank()) {
                    setArtworkUri(Uri.parse(artworkUrl))
                }
            }
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(streamUrl)
            .setUri(Uri.parse(streamUrl))
            .setMediaMetadata(metadata)
            .build()

        _playbackState.value = PlaybackState(
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            isPlaying = true,
            hasMedia = true
        )

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
        _playbackState.value =
            _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        if (_playbackState.value.hasMedia) {
            player.play()
            _playbackState.value =
                _playbackState.value.copy(isPlaying = true)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _playbackState.value = PlaybackState()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun release() {
        player.release()
    }
}

