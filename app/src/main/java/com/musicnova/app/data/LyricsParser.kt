
package com.musicnova.app.data.lyrics

private val timestampRegex =
    Regex("""\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?]""")

private val offsetRegex =
    Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

fun parseLyrics(
    syncedLyrics: String?,
    plainLyrics: String?,
    source: String? = "LRCLIB"
): LyricsResult {

    val synced = syncedLyrics?.trim().orEmpty()
    val plain = plainLyrics?.trim().orEmpty()

    if (synced.isBlank()) {
        if (plain.isBlank()) return LyricsResult.Empty

        val plainLines = plain.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map {
                LyricLine(
                    timeMs = 0L,
                    text = it
                )
            }

        return LyricsResult.Success(
            lines = plainLines,
            isSynced = false,
            isWordSynced = false,
            plainLyrics = plain,
            source = source
        )
    }

    var offsetMs = 0L
    val parsed = mutableListOf<LyricLine>()

    synced.lines().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) return@forEach

        offsetRegex.find(line)?.let {
            offsetMs = it.groupValues[1].toLongOrNull() ?: 0L
            return@forEach
        }

        val timestamps = timestampRegex.findAll(line).toList()
        if (timestamps.isEmpty()) return@forEach

        val lyricText = timestampRegex.replace(line, "")
            .trim()

        if (lyricText.isBlank()) return@forEach

        timestamps.forEach { match ->
            val minutes =
                match.groupValues[1].toLongOrNull() ?: 0L
            val seconds =
                match.groupValues[2].toLongOrNull() ?: 0L

            val fraction = match.groupValues[3]
            val millis = when (fraction.length) {
                1 -> (fraction.toLongOrNull() ?: 0L) * 100L
                2 -> (fraction.toLongOrNull() ?: 0L) * 10L
                3 -> fraction.toLongOrNull() ?: 0L
                else -> 0L
            }

            val timestamp =
                (minutes * 60_000L) +
                (seconds * 1_000L) +
                millis + offsetMs

            parsed.add(
                LyricLine(
                    timeMs = timestamp.coerceAtLeast(0L),
                    text = lyricText
                )
            )
        }
    }

    val sortedLines = parsed.sortedBy { it.timeMs }

    if (sortedLines.isEmpty()) {
        return if (plain.isNotBlank()) {
            parseLyrics(null, plain, source)
        } else {
            LyricsResult.Empty
        }
    }

    val linesWithDuration = sortedLines.mapIndexed { index, lyric ->
        val nextTime = sortedLines
            .getOrNull(index + 1)?.timeMs

        lyric.copy(
            durationMs = nextTime
                ?.minus(lyric.timeMs)
                ?.coerceAtLeast(0L) ?: 0L
        )
    }

    val instrumental = linesWithDuration.any {
        it.text.contains("[instrumental]", ignoreCase = true) ||
        it.text.contains("(instrumental)", ignoreCase = true)
    }

    return LyricsResult.Success(
        lines = linesWithDuration,
        isSynced = true,
        isWordSynced = false,
        plainLyrics = plain.ifBlank { null },
        isInstrumental = instrumental,
        source = source
    )
}
