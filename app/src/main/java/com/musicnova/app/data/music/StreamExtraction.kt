
package com.musicnova.app.data.music

import java.io.IOException
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class StreamExtraction(
    private val extractor: YouTubeStreamExtractor =
        YouTubeStreamExtractor(),
    private val innerTubeX: InnerTubeXStreamExtractor =
        InnerTubeXStreamExtractor()
) {

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class CachedStream(
        val stream: ResolvedAudioStream,
        val savedAt: Long
    )

    private val cacheLock = Any()

    private val streamCache =
        LinkedHashMap<String, CachedStream>(16, 0.75f, true)

    private val inFlight =
        ConcurrentHashMap<String, Deferred<ResolvedAudioStream>>()

    private val cacheLimit = 12
    private val cacheTtlMs = 90_000L

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
            throw IOException("YouTube video ID is empty")
        }

        getCached(videoId)?.let { return it }

        val task = inFlight.computeIfAbsent(videoId) {
            scope.async {
                resolveUncached(videoId)
            }
        }

        try {
            val result = task.await()
            saveCached(videoId, result)
            return result
        } finally {
            inFlight.remove(videoId, task)
        }
    }

    fun prefetch(videoId: String) {
        if (videoId.isBlank()) return

        if (getCached(videoId) != null) return

        if (inFlight.containsKey(videoId)) return

        scope.launch {
            try {
                resolve(videoId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.w(
                    "MusicNova",
                    "Stream prefetch failed for $videoId",
                    error
                )
            }
        }
    }

    private suspend fun resolveUncached(
        videoId: String
    ): ResolvedAudioStream {

        var innerTubeError: Exception? = null

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

    suspend fun resolveWithNewPipe(
        videoId: String
    ): ResolvedAudioStream {

        if (videoId.isBlank()) {
            throw IOException("YouTube video ID is empty")
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

    private fun getCached(
        videoId: String
    ): ResolvedAudioStream? {

        synchronized(cacheLock) {
            val entry = streamCache[videoId] ?: return null

            if (System.currentTimeMillis() - entry.savedAt >
                cacheTtlMs
            ) {
                streamCache.remove(videoId)
                return null
            }

            return entry.stream
        }
    }

    private fun saveCached(
        videoId: String,
        stream: ResolvedAudioStream
    ) {
        synchronized(cacheLock) {
            streamCache[videoId] = CachedStream(
                stream = stream,
                savedAt = System.currentTimeMillis()
            )

            while (streamCache.size > cacheLimit) {
                val oldest = streamCache.keys.firstOrNull()
                    ?: break

                streamCache.remove(oldest)
            }
        }
    }
}

