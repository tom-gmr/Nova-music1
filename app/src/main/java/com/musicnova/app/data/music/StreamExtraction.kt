package com.musicnova.app.data.music

import java.io.IOException
import kotlinx.coroutines.CancellationException

/**
 * Resolves audio streams using InnerTubeX first
 * and NewPipe as a fallback.
 */
class StreamExtraction(
    private val extractor: YouTubeStreamExtractor =
        YouTubeStreamExtractor(),
    private val innerTubeX: InnerTubeXStreamExtractor =
        InnerTubeXStreamExtractor()
) {

    suspend fun resolve(
        track: YouTubeMusicTrack
    ): ResolvedAudioStream {

        if (track.isArtist) {
            throw IOException(
                "Artist profiles cannot be played as songs"
            )
        }

        if (track.videoId.isBlank()) {
            throw IOException(
                "Song video ID is missing"
            )
        }

        return resolve(track.videoId)
    }

    suspend fun resolve(
        videoId: String
    ): ResolvedAudioStream {

        if (videoId.isBlank()) {
            throw IOException(
                "YouTube video ID is empty"
            )
        }

        var innerTubeError: Exception? = null

        // First attempt: InnerTubeX
        try {
            val result = innerTubeX.resolve(videoId)

            if (result != null) {
                return result
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            innerTubeError = error
        }

        // Fallback attempt: NewPipe
        try {
            return resolveWithNewPipe(videoId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val failure = IOException(
                "Both InnerTubeX and NewPipe failed to resolve the audio stream.",
                error
            )

            innerTubeError?.let {
                failure.addSuppressed(it)
            }

            throw failure
        }
    }

    /**
     * Direct NewPipe fallback.
     * This will be used when the current playback stream fails.
     */
    suspend fun resolveWithNewPipe(
        videoId: String
    ): ResolvedAudioStream {

        if (videoId.isBlank()) {
            throw IOException(
                "YouTube video ID is empty"
            )
        }

        return try {
            extractor.resolveAudioStream(videoId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw IOException(
                "NewPipe fallback stream failed.",
                error
            )
        }
    }
}
