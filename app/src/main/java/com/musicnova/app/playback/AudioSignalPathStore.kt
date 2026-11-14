package com.musicnova.app.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AudioSignalPathStore {

    private val _state =
        MutableStateFlow(AudioSignalPathState())

    val state: StateFlow<AudioSignalPathState> =
        _state.asStateFlow()

    fun updateSource(source: AudioSourceInfo) {
        _state.update {
            it.copy(source = source)
        }
    }

    fun updateSystemInfo(info: AudioSignalPathState) {
        _state.update {
            it.copy(
                output = info.output,
                device = info.device,
                diagnosticError = info.diagnosticError
            )
        }
    }

    fun updateDsp(dsp: AudioDspInfo) {
        _state.update {
            it.copy(dsp = dsp)
        }
    }

    fun resetSource() {
        _state.update {
            it.copy(source = AudioSourceInfo())
        }
    }
}
