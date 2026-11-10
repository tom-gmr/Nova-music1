package com.musicnova.app.ui.player

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.musicnova.app.data.lyrics.LyricLine
import com.musicnova.app.data.lyrics.LyricSyllable

private val KaraokeActive = Color(0xFFD8C5FF)
private val KaraokeSung = Color(0xFFFFFFFF)
private val KaraokeUpcoming = Color(0xFF777080)

@Composable
internal fun KaraokeLyricText(
    line: LyricLine,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    textAlign: TextAlign = TextAlign.Center,
    activeColor: Color = KaraokeActive,
    sungColor: Color = KaraokeSung,
    upcomingColor: Color = KaraokeUpcoming
) {
    val syllables = line.syllables
        .filter { it.text.isNotBlank() && !it.isBackground }

    val displayText = if (syllables.isNotEmpty()) {
        syllables.joinToString("") { it.text }
    } else {
        line.text
    }

    val annotatedText = buildAnnotatedString {
        append(displayText)

        if (syllables.isEmpty()) {
            addStyle(
                SpanStyle(color = sungColor),
                0,
                length
            )
        } else {
            var charStart = 0

            syllables.forEach { syllable ->
                val charEnd = (charStart + syllable.text.length)
                    .coerceAtMost(length)

                if (charStart < charEnd) {
                    val color = when {
                        currentPositionMs < syllable.timeMs ->
                            upcomingColor

                        syllable.durationMs <= 0L ||
                            currentPositionMs >=
                            syllable.timeMs + syllable.durationMs ->
                            sungColor

                        else -> activeColor
                    }

                    addStyle(
                        SpanStyle(
                            color = color,
                            fontWeight = FontWeight.SemiBold
                        ),
                        charStart,
                        charEnd
                    )
                }

                charStart = charEnd
            }
        }
    }

    Text(
        text = annotatedText,
        modifier = modifier,
        style = style,
        textAlign = textAlign
    )
}

