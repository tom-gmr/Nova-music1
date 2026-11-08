package com.musicnova.app.data.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ResolvedAudioStream(
    val videoId: String,
    val url: String,
    val mimeType: String?,
    val bitrate: Int,
    val requestHeaders: Map<String, String>
)

class YouTubeStreamExtractor {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val probeHttp = http.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var initialized = false

    private fun initialize() {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            NewPipe.init(NovaDownloader(http))
            initialized = true
        }
    }

    suspend fun resolveAudioStream(
        videoId: String
    ): ResolvedAudioStream = withContext(Dispatchers.IO) {

        require(videoId.isNotBlank()) {
            "YouTube video ID is empty"
        }

        initialize()

        val info = try {
            StreamInfo.getInfo(
                ServiceList.YouTube,
                "https://www.youtube.com/watch?v=$videoId"
            )
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) {
                throw error
            }

            throw IOException(
                "YouTube stream extraction failed",
                error
            )
        }

        val candidates = info.audioStreams
            .filter { stream ->
                stream.content.startsWith("https://") &&
                    isSupportedMime(stream.format?.mimeType)
            }
            .distinctBy { it.content }
            .sortedWith(
                compareBy<AudioStream> {
                    val mime = it.format?.mimeType.orEmpty()
                    if (mime.contains("mp4", true)) 0 else 1
                }.thenByDescending {
                    maxOf(it.averageBitrate, it.bitrate)
                }
            )

        if (candidates.isEmpty()) {
            throw IOException("No audio stream candidates found")
        }

        var lastError: Exception? = null

        for (stream in candidates) {

            currentCoroutineContext().ensureActive()

            val playable = try {
                validateStream(
                    videoId = videoId,
                    stream = stream
                )
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) {
                    throw error
                }

                lastError = error
                false
            }

            if (playable) {
                return@withContext ResolvedAudioStream(
                    videoId = videoId,
                    url = stream.content,
                    mimeType = stream.format?.mimeType,
                    bitrate = maxOf(
                        stream.averageBitrate,
                        stream.bitrate
                    ),
                    requestHeaders = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Origin" to "https://www.youtube.com",
                        "Referer" to
                            "https://www.youtube.com/watch?v=$videoId"
                    )
                )
            }
        }

        throw IOException(
            "No playable audio stream found after testing " +
                "${candidates.size} candidates",
            lastError
        )
    }

    private fun isSupportedMime(mimeType: String?): Boolean {
        if (mimeType.isNullOrBlank()) return true

        val mime = mimeType.lowercase()

        return mime.startsWith("audio/") ||
            mime == "application/octet-stream" ||
            mime.contains("mp4")
    }

    private fun validateStream(
        videoId: String,
        stream: AudioStream
    ): Boolean {

        val request = okhttp3.Request.Builder()
            .url(stream.content)
            .get()
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://www.youtube.com/watch?v=$videoId")
            .header("Origin", "https://www.youtube.com")
            .header("Accept", "audio/*,*/*;q=0.8")
            .header("Accept-Encoding", "identity")
            .header("Range", "bytes=0-1023")
            .build()

        return try {
            probeHttp.newCall(request)
                .execute()
                .use { response ->

                    val statusValid =
                        response.code == 200 ||
                            response.code == 206

                    if (!statusValid) {
                        return@use false
                    }

                    val contentType = response.header("Content-Type")
                        ?.substringBefore(";")
                        ?.trim()
                        ?.lowercase()
                        .orEmpty()

                    val invalidContent =
                        contentType.contains("text/html") ||
                            contentType.contains("application/json") ||
                            contentType.startsWith("text/")

                    !invalidContent
                }
        } catch (error: Exception) {
            if (error is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            false
        }
    }

    private class NovaDownloader(
        private val client: OkHttpClient
    ) : Downloader() {

        override fun execute(request: Request): Response {

            val builder = okhttp3.Request.Builder()
                .url(request.url())
                .method(
                    request.httpMethod(),
                    request.dataToSend()?.toRequestBody()
                )

            request.headers().forEach { (name, values) ->
                values.forEach { value ->
                    builder.addHeader(name, value)
                }
            }

            if (request.headers().keys.none {
                    it.equals("User-Agent", true)
                }
            ) {
                builder.header("User-Agent", USER_AGENT)
            }

            if (request.headers().keys.none {
                    it.equals("Accept-Language", true)
                }
            ) {
                builder.header(
                    "Accept-Language",
                    "en-US,en;q=0.9"
                )
            }

            return client.newCall(builder.build())
                .execute()
                .use { response ->

                    Response(
                        response.code,
                        response.message,
                        response.headers.names()
                            .associateWith {
                                response.headers.values(it)
                            },
                        response.body?.string().orEmpty(),
                        response.request.url.toString()
                    )
                }
        }
    }

    companion object {

        private const val PROBE_TIMEOUT_MS = 10000L

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Safari/537.36"
    }
}
