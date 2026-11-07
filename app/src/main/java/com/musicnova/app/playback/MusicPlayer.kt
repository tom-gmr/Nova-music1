package com.musicnova.app.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

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

    val exoPlayer: ExoPlayer
        get() = player

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

        httpDataSourceFactory.setDefaultRequestProperties(requestHeaders)

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

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() = player.pause()

    fun resume() = player.play()

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun release() = player.release()
}
