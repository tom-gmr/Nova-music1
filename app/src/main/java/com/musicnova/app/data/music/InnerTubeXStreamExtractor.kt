package com.musicnova.app.data.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume

/**
 * InnerTubeX resolver for Music Nova.
 *
 * Reflection keeps this adapter compatible with the library's
 * Kotlin metadata while preserving the existing NewPipe extractor.
 */
class InnerTubeXStreamExtractor {

    private val runtime: RuntimeStack by lazy {
        createRuntime()
    }

    /**
     * Returns null when InnerTubeX cannot find a usable stream.
     * StreamExtraction will use NewPipe as the fallback later.
     */
    suspend fun resolve(videoId: String): ResolvedAudioStream? =
        withContext(Dispatchers.IO) {

            require(videoId.isNotBlank()) {
                "YouTube video ID is empty"
            }

            try {
                val stack = runtime

                val hints = Class.forName(
                    "com.metrolist.innertubex.extraction.ContentHints"
                ).getDeclaredConstructor().newInstance()

                val capabilityMethod =
                    hints.javaClass.methods.firstOrNull {
                        it.name == "withStreamCapabilities" &&
                            it.parameterCount == 3
                    } ?: return@withContext null

                // Direct/HLS streams only; no segmented SABR transport.
                val audioHints = capabilityMethod.invoke(
                    hints,
                    true,
                    false,
                    false
                )

                val quality = enumValue(
                    "com.metrolist.innertubex.extraction.AudioQuality",
                    "HIGH"
                )

                val extractMethod =
                    stack.extractor.javaClass.methods.firstOrNull {
                        it.name == "extract" &&
                            it.parameterCount == 6
                    } ?: return@withContext null

                val result = invokeSuspend(
                    receiver = stack.extractor,
                    method = extractMethod,
                    arguments = arrayOf(
                        videoId,
                        audioHints,
                        emptySet<String>(),
                        quality,
                        UUID.randomUUID().toString().replace("-", "")
                    )
                ) ?: return@withContext null

                result.toResolvedAudioStream(videoId)

            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                null
            } catch (error: LinkageError) {
                null
            }
        }

    private fun Any.toResolvedAudioStream(
        videoId: String
    ): ResolvedAudioStream? {

        val url = readValue(this, "getAudioUrl") as? String
            ?: return null

        if (!url.startsWith("https://", ignoreCase = true)) {
            return null
        }

        if (url.startsWith("sabr:", ignoreCase = true)) {
            return null
        }

        val mimeType = readValue(this, "getMimeType") as? String
        val cleanMime = mimeType
            ?.substringBefore(";")
            ?.trim()
            ?.lowercase()

        val isHls =
            cleanMime == "application/x-mpegurl" ||
                cleanMime == "application/vnd.apple.mpegurl" ||
                url.substringBefore("?").endsWith(".m3u8", true)

        if (!isHls && cleanMime?.startsWith("audio/") != true) {
            return null
        }

        val rawHeaders = readValue(this, "getHeaders") as? Map<*, *>

        val headers = rawHeaders
            ?.mapNotNull { (key, value) ->
                val name = key as? String
                val headerValue = value as? String

                if (name.isNullOrBlank() || headerValue.isNullOrBlank()) {
                    null
                } else {
                    name to headerValue
                }
            }
            ?.toMap()
            .orEmpty()

        val bitrate = (
            readValue(this, "getBitrate") as? Number
        )?.toInt() ?: 0

        return ResolvedAudioStream(
            videoId = videoId,
            url = url,
            mimeType = cleanMime,
            bitrate = bitrate,
            requestHeaders = headers
        )
    }

    private fun createRuntime(): RuntimeStack {

        val httpClient = createKtorHttpClient()

        val repository = callCompanion(
            "com.metrolist.innertubex.cipher.PlayerConfigRepository",
            "disabled"
        )

        val configStore = constructWithDefaults(
            "com.metrolist.innertubex.cipher.RemotePlayerConfigStore",
            arrayOf(httpClient, repository),
            defaultMask = 4
        )

        val innerTube = constructWithDefaults(
            "com.metrolist.innertubex.InnerTube",
            arrayOf(httpClient),
            defaultMask = 6
        )

        val cipher = constructWithDefaults(
            "com.metrolist.innertubex.cipher.YouTubeCipherService",
            arrayOf(httpClient, configStore),
            defaultMask = 4
        )

        val parser = constructWithDefaults(
            "com.metrolist.innertubex.extraction.YtConfigParserImpl",
            arrayOf(httpClient, innerTube, configStore),
            defaultMask = 24
        )

        val extractor = constructWithDefaults(
            "com.metrolist.innertubex.extraction.InnerTubeExtractor",
            arrayOf(parser, cipher, innerTube),
            defaultMask = 120
        )

        return RuntimeStack(
            innerTube = innerTube,
            extractor = extractor
        )
    }

    private fun createKtorHttpClient(): Any {

        val engines = listOf(
            "io.ktor.client.engine.okhttp.OkHttp",
            "io.ktor.client.engine.cio.CIO"
        )

        var lastError: Throwable? = null

        for (engineClass in engines) {
            try {
                val engine = Class.forName(engineClass)
                    .getField("INSTANCE")
                    .get(null)

                val factory = Class.forName(
                    "io.ktor.client.HttpClientKt"
                ).methods.first {
                    it.name == "HttpClient" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[0].name ==
                        "io.ktor.client.engine.HttpClientEngineFactory"
                }

                return factory.invoke(
                    null,
                    engine,
                    { _: Any? -> Unit }
                )!!

            } catch (error: Throwable) {
                lastError = error
            }
        }

        throw IllegalStateException(
            "Unable to create Ktor HTTP client",
            lastError
        )
    }

    private fun constructWithDefaults(
        className: String,
        required: Array<Any?>,
        defaultMask: Int
    ): Any {

        val type = Class.forName(className)

        val constructor = type.declaredConstructors.firstOrNull {
            candidate ->

            candidate.parameterCount >= required.size + 2 &&
                candidate.parameterTypes[
                    candidate.parameterCount - 2
                ] == Int::class.javaPrimitiveType &&
                candidate.parameterTypes.last().name ==
                "kotlin.jvm.internal.DefaultConstructorMarker"
        } ?: throw IllegalStateException(
            "Compatible constructor not found: $className"
        )

        constructor.isAccessible = true

        val arguments = arrayOfNulls<Any?>(
            constructor.parameterCount
        )

        required.copyInto(arguments)

        arguments[constructor.parameterCount - 2] = defaultMask

        return constructor.newInstance(*arguments)
    }

    private fun callCompanion(
        className: String,
        methodName: String
    ): Any {

        val type = Class.forName(className)
        val companion = type.getField("Companion").get(null)

        return companion.javaClass
            .getMethod(methodName)
            .invoke(companion)
    }

    private fun enumValue(
        className: String,
        value: String
    ): Any {

        val constants = Class.forName(className).enumConstants
            ?: throw IllegalStateException("Enum not found: $className")

        return constants.first {
            (it as Enum<*>).name == value
        }
    }

    private fun readValue(
        instance: Any,
        getter: String
    ): Any? {

        return instance.javaClass.methods
            .firstOrNull {
                it.name == getter && it.parameterCount == 0
            }
            ?.invoke(instance)
    }

    private suspend fun invokeSuspend(
        receiver: Any,
        method: Method,
        arguments: Array<Any?>
    ): Any? = suspendCancellableCoroutine { continuation ->

        val completion = object : Continuation<Any?> {

            override val context: CoroutineContext =
                continuation.context

            override fun resumeWith(result: Result<Any?>) {
                continuation.resumeWith(result)
            }
        }

        try {
            val result = method.invoke(
                receiver,
                *(arguments + completion)
            )

            if (
                result !== COROUTINE_SUSPENDED &&
                continuation.isActive
            ) {
                continuation.resume(result)
            }

        } catch (error: InvocationTargetException) {

            if (continuation.isActive) {
                continuation.resumeWith(
                    Result.failure(error.targetException)
                )
            }

        } catch (error: Throwable) {

            if (continuation.isActive) {
                continuation.resumeWith(
                    Result.failure(error)
                )
            }
        }
    }

    private data class RuntimeStack(
        val innerTube: Any,
        val extractor: Any
    )
}
