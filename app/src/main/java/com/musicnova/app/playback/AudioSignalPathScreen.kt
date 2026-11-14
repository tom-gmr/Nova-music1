package com.musicnova.app.playback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AudioSignalPathScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val reader = remember(context) {
        AudioSignalPathReader(context)
    }

    val state by AudioSignalPathStore.state.collectAsState()

    fun refresh() {
        AudioSignalPathStore.updateSystemInfo(
            reader.read()
        )
    }

    LaunchedEffect(reader) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Signal Path",
                style = MaterialTheme.typography.headlineMedium
            )

            TextButton(onClick = onBack) {
                Text("Back")
            }
        }

        Button(
            onClick = { refresh() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Diagnostics")
        }

        SignalSection("SOURCE AUDIO") {
            SignalRow("Title", state.source.title)
            SignalRow("Artist", state.source.artist)
            SignalRow("Format", state.source.mimeType)
            SignalRow("Sample Rate", state.source.sampleRateHz?.let { "$it Hz" })
            SignalRow("Channels", state.source.channelCount?.toString())
            SignalRow("Bitrate", state.source.bitrate?.let { "$it bps" })
        }

        SignalSection("AUDIO OUTPUT") {
            SignalRow("Output Rate", state.output.sampleRateHz?.let { "$it Hz" })
            SignalRow("Buffer", state.output.bufferFrames?.let { "$it frames" })
            SignalRow("Device", state.device.name)
            SignalRow("Device Type", state.device.type)
            SignalRow(
                "Connected",
                if (state.device.isConnected) "Yes" else "Unknown"
            )
        }

        SignalSection("DSP STATUS") {
            SignalRow("Equalizer", enabledText(state.dsp.equalizerEnabled))
            SignalRow("Tone Control", enabledText(state.dsp.toneControlEnabled))
            SignalRow("Crossfade", enabledText(state.dsp.crossfadeEnabled))
            SignalRow(
                "Normalization",
                enabledText(state.dsp.normalizationEnabled)
            )
            SignalRow(
                "Preamp",
                state.dsp.preampDb?.let { "$it dB" }
            )
        }

        SignalSection("BIT-PERFECT STATUS") {
            SignalRow(
                "Status",
                when (state.bitPerfect) {
                    true -> "Verified"
                    false -> "Not bit-perfect"
                    null -> "Not verified"
                }
            )
            SignalRow("Reason", state.bitPerfectReason)
        }

        state.diagnosticError?.let {
            Text(
                text = "Diagnostic: $it",
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "Note: System output information does not guarantee " +
                "the exact format reaching the audio hardware.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SignalSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            content()
        }
    }
}

@Composable
private fun SignalRow(
    label: String,
    value: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value ?: "Unavailable")
    }
}

private fun enabledText(enabled: Boolean): String {
    return if (enabled) "Enabled" else "Disabled"
}
