package com.musicnova.app.data.music

import java.io.IOException

/**
 * Connects YouTube Music search results
 * with the YouTube audio stream extractor.
 */
class StreamExtraction(
    private val extractor: YouTubeStreamExtractor =
        YouTubeStreamExtractor()
) {

    /**
     * Resolves a playable audio stream for a song.
     */
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

    /**
     * Resolves a playable audio stream using video ID.
     */
    suspend fun resolve(
        videoId: String
    ): ResolvedAudioStream {

        if (videoId.isBlank()) {
            throw IOException(
                "YouTube video ID is empty"
            )
        }

        return try {
            extractor.resolveAudioStream(videoId)
        } catch (error: Exception) {
            throw IOException(
                "Unable to resolve audio stream: ${error.message}",
                error
            )
        }
    }
}
