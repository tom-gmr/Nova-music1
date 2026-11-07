
package com.musicnova.app.data.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
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
        .followRedirects(true)
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
            throw IOException(
                "YouTube stream extraction failed",
                error
            )
        }

        val stream = info.audioStreams
            .filter { it.content.startsWith("https://") }
            .sortedWith(
                compareBy<org.schabi.newpipe.extractor.stream.AudioStream> {
                    val mime = it.format?.mimeType.orEmpty()
                    if (mime.contains("mp4", true)) 0 else 1
                }.thenByDescending {
                    maxOf(it.averageBitrate, it.bitrate)
                }
            )
            .firstOrNull()
            ?: throw IOException("No playable audio stream found")

        ResolvedAudioStream(
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
                "Referer" to "https://www.youtube.com/watch?v=$videoId"
            )
        )
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
                builder.header("Accept-Language", "en-US,en;q=0.9")
            }

            return client.newCall(builder.build())
                .execute()
                .use { response ->

                    Response(
                        response.code,
                        response.message,
                        response.headers.names()
                            .associateWith { response.headers.values(it) },
                        response.body?.string().orEmpty(),
                        response.request.url.toString()
                    )
                }
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36"
    }
}
