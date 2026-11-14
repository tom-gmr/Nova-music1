
package com.musicnova.app.playback

import android.content.Context
import android.media.AudioManager
import android.os.Build

class AudioSignalPathReader(
    context: Context
) {
    private val appContext = context.applicationContext

    fun read(): AudioSignalPathState {
        val audioManager = appContext.getSystemService(
            Context.AUDIO_SERVICE
        ) as? AudioManager
            ?: return AudioSignalPathState(
                diagnosticError = "AudioManager unavailable"
            )

        val sampleRate = audioManager
            .getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull()

        val bufferFrames = audioManager
            .getProperty(
                AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER
            )
            ?.toIntOrNull()

        val outputDevices = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        ) {
            audioManager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            ).toList()
        } else {
            emptyList()
        }

        val selectedDevice = outputDevices.firstOrNull {
            it.type != android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } ?: outputDevices.firstOrNull()

        val deviceInfo = selectedDevice?.let { device ->
            AudioDeviceInfo(
                name = device.productName?.toString(),
                type = deviceTypeName(device.type),
                isConnected = true
            )
        } ?: AudioDeviceInfo()

        return AudioSignalPathState(
            output = AudioOutputInfo(
                sampleRateHz = sampleRate,
                bufferFrames = bufferFrames,
                channelCount = null,
                encoding = null,
                latencyMs = null
            ),
            device = deviceInfo
        )
    }

    private fun deviceTypeName(type: Int): String {
        return when (type) {
            android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ->
                "Built-in speaker"

            android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ->
                "Wired headphones"

            android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ->
                "Wired headset"

            android.media.AudioDeviceInfo.TYPE_USB_DEVICE ->
                "USB audio device"

            android.media.AudioDeviceInfo.TYPE_USB_HEADSET ->
                "USB headset"

            android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ->
                "Bluetooth audio"

            android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ->
                "Bluetooth headset"

            else -> "Other audio device"
        }
    }
}
