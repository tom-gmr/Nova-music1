package com.musicnova.app.playback

import androidx.compose.runtime.Immutable

@Immutable
data class AudioSourceInfo(
    val title: String = "",
    val artist: String = "",
    val mimeType: String? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val bitDepth: Int? = null,
    val bitrate: Int? = null
)

@Immutable
data class AudioDspInfo(
    val equalizerEnabled: Boolean = false,
    val toneControlEnabled: Boolean = false,
    val crossfadeEnabled: Boolean = false,
    val normalizationEnabled: Boolean = false,
    val preampDb: Float? = null,
    val activeEffects: List<String> = emptyList()
)

@Immutable
data class AudioOutputInfo(
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val encoding: String? = null,
    val bufferFrames: Int? = null,
    val latencyMs: Long? = null
)

@Immutable
data class AudioDeviceInfo(
    val name: String? = null,
    val type: String? = null,
    val isConnected: Boolean = false
)

@Immutable
data class AudioSignalPathState(
    val source: AudioSourceInfo = AudioSourceInfo(),
    val dsp: AudioDspInfo = AudioDspInfo(),
    val output: AudioOutputInfo = AudioOutputInfo(),
    val device: AudioDeviceInfo = AudioDeviceInfo(),
    val bitPerfect: Boolean? = null,
    val bitPerfectReason: String? = null,
    val isLoading: Boolean = false,
    val diagnosticError: String? = null
)
