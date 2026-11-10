package com.musicnova.app.data.lyrics

fun isRtlText(text: CharSequence?): Boolean {
    if (text.isNullOrBlank()) return false

    var rtl = 0
    var ltr = 0
    var firstStrongRtl: Boolean? = null
    var i = 0

    while (i < text.length) {
        val codePoint = Character.codePointAt(text, i)

        when (Character.getDirectionality(codePoint)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> {
                if (firstStrongRtl == null) firstStrongRtl = true
                rtl++
            }

            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> {
                if (firstStrongRtl == null) firstStrongRtl = false
                ltr++
            }
        }

        i += Character.charCount(codePoint)
    }

    return firstStrongRtl == true || (rtl > 0 && rtl >= ltr)
}

data class LyricSyllable(
    val timeMs: Long,
    val durationMs: Long,
    val text: String,
    val isBackground: Boolean = false,
    val appendToPrevious: Boolean = false
)

enum class VocalRole {
    LEAD_LEFT,
    DUET_RIGHT,
    UNISON_CENTER,
    BACKGROUND_SUBLINE
}

data class LyricLine(
    val timeMs: Long,
    val durationMs: Long = 0L,
    val text: String,
    val syllables: List<LyricSyllable> = emptyList(),
    val transliteration: String? = null,
    val transliterationSyllables: List<LyricSyllable> = emptyList(),
    val role: VocalRole = VocalRole.LEAD_LEFT
) {
    val hasSyllables: Boolean
        get() = syllables.isNotEmpty()

    val isRtl: Boolean
        get() = isRtlText(text) ||
            syllables.any { isRtlText(it.text) }
}

enum class LyricsSyncType(val label: String) {
    WORD_SYNC("Word-sync"),
    LINE_SYNC("Line-sync"),
    NO_SYNC("No sync"),
    NO_LYRICS("No lyrics"),
    CHECKING("Checking...")
}

sealed interface LyricsResult {
    data class Success(
        val lines: List<LyricLine>,
        val isSynced: Boolean,
        val isWordSynced: Boolean = false,
        val plainLyrics: String? = null,
        val isInstrumental: Boolean = false,
        val source: String? = null
    ) : LyricsResult

    data object Empty : LyricsResult

    data class Error(
        val message: String
    ) : LyricsResult
}
