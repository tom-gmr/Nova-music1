
package com.musicnova.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.musicnova.app.MainActivity
import java.io.File
import androidx.media3.common.Tracks
import androidx.media3.common.Player

class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var streamCache: SimpleCache? = null

    private lateinit var httpDataSourceFactory:
        DefaultHttpDataSource.Factory

    override fun onCreate() {
        super.onCreate()

        httpDataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

        streamCache = SimpleCache(
            File(cacheDir, "music_stream_cache"),
            LeastRecentlyUsedCacheEvictor(
                100L * 1024 * 1024
            ),
            StandaloneDatabaseProvider(this)
        )

        val cacheDataSourceFactory =
            CacheDataSource.Factory()
                .setCache(streamCache!!)
                .setUpstreamDataSourceFactory(
                    httpDataSourceFactory
                )
                .setFlags(
                    CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
                )

        val exoPlayer =
            ExoPlayer.Builder(this)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(
                        cacheDataSourceFactory
                    )
                )
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(
                            C.AUDIO_CONTENT_TYPE_MUSIC
                        )
                        .build(),
                    true
                )
                .build()

        player = exoPlayer
        exoPlayer.addListener(
    object : Player.Listener {

        override fun onTracksChanged(tracks: Tracks) {
            val audioGroup = tracks.groups.firstOrNull {
                it.type == C.TRACK_TYPE_AUDIO && it.isSelected
            }

            val audioFormat = audioGroup?.let { group ->
                (0 until group.length)
                    .firstOrNull { group.isTrackSelected(it) }
                    ?.let { group.getTrackFormat(it) }
            }

            if (audioFormat == null) {
                AudioSignalPathStore.resetSource()
                return
            }

            val metadata =
                exoPlayer.currentMediaItem?.mediaMetadata

            AudioSignalPathStore.updateSource(
                AudioSourceInfo(
                    title = metadata?.title?.toString().orEmpty(),
                    artist = metadata?.artist?.toString().orEmpty(),
                    mimeType = audioFormat.sampleMimeType,
                    sampleRateHz = audioFormat.sampleRate
                        .takeIf { it > 0 },
                    channelCount = audioFormat.channelCount
                        .takeIf { it > 0 },
                    bitrate = audioFormat.bitrate
                        .takeIf { it > 0 }
                )
            )
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            AudioSignalPathStore.resetSource()
        }
    }
)

        val activityIntent =
            Intent(this, MainActivity::class.java)

        val activityPendingIntent =
            PendingIntent.getActivity(
                this,
                1001,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        mediaSession =
            MediaSession.Builder(this, exoPlayer)
                .setSessionActivity(activityPendingIntent)
                .setCallback(
                    object : MediaSession.Callback {

                        override fun onAddMediaItems(
                            mediaSession: MediaSession,
                            controller: MediaSession.ControllerInfo,
                            mediaItems: List<MediaItem>
                        ): ListenableFuture<List<MediaItem>> {

                            val extras =
                                mediaItems.firstOrNull()
                                    ?.mediaMetadata
                                    ?.extras

                            val names =
                                extras?.getStringArrayList(
                                    "nova_header_names"
                                )

                            val values =
                                extras?.getStringArrayList(
                                    "nova_header_values"
                                )

                            val headers =
                                if (
                                    names != null &&
                                    values != null &&
                                    names.size == values.size
                                ) {
                                    names.indices.associate {
                                        names[it] to values[it]
                                    }
                                } else {
                                    emptyMap()
                                }

                            httpDataSourceFactory
                                .setDefaultRequestProperties(
                                    headers
                                )

                            return Futures.immediateFuture(
                                mediaItems
                            )
                        }
                    }
                )
                .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {
        if (player?.isPlaying != true) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }

        mediaSession = null
        player = null

        streamCache?.release()
        streamCache = null

        super.onDestroy()
    }
}
