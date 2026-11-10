
package com.musicnova.app.ui.player

import com.musicnova.app.data.lyrics.LyricLine
import com.musicnova.app.data.lyrics.LyricSyllable
import com.musicnova.app.data.lyrics.VocalRole
import com.musicnova.app.data.lyrics.isRtlText

internal sealed interface LyricsItem {
    val startMs: Long
    val endMs: Long
    val stanzaGapAfter: Boolean
}

internal data class StagedVocal(
    val lineIndex: Int,
    val startMs: Long,
    val endMs: Long?,
    val text: String,
    val units: List<LyricSyllable>,
    val isRtl: Boolean
) {
    val isWordSynced: Boolean
        get() = units.isNotEmpty()
}

internal data class LyricLineGroup(
    val lead: StagedVocal?,
    val backgrounds: List<StagedVocal>,
    val role: VocalRole,
    override val startMs: Long,
    override val endMs: Long,
    val endKnown: Boolean,
    override val stanzaGapAfter: Boolean = false
) : LyricsItem {
    val seekMs: Long
        get() = lead?.startMs ?: startMs

    val isWordSynced: Boolean
        get() = lead?.isWordSynced == true ||
            backgrounds.any { it.isWordSynced }
}

internal data class LyricInterlude(
    override val startMs: Long,
    override val endMs: Long,
    val role: VocalRole,
    override val stanzaGapAfter: Boolean = false
) : LyricsItem

internal class LyricsStage(
    val items: List<LyricsItem>,
    val starts: LongArray,
    val isStaged: Boolean
) {
    fun activeIndex(positionMs: Long): Int {
        var low = 0
        var high = starts.size

        while (low < high) {
            val mid = (low + high) ushr 1
            if (starts[mid] <= positionMs) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low - 1
    }

    fun nextStartAfter(positionMs: Long): Long? {
        val index = activeIndex(positionMs) + 1
        return starts.getOrNull(index)
    }

    companion object {
        val Empty = LyricsStage(
            emptyList(),
            LongArray(0),
            false
        )
    }
}

private const val INTERLUDE_MIN_MS = 3500L
private const val STANZA_GAP_MS = 2500L
private const val BACKING_ATTACH_MS = 3000L

private fun LyricLine.knownEnd(): Long? {
    val syllableEnd = syllables.maxOfOrNull {
        it.timeMs + it.durationMs
    }

    val lineEnd = if (durationMs > 0L) {
        timeMs + durationMs
    } else null

    return when {
        lineEnd != null && syllableEnd != null ->
            maxOf(lineEnd, syllableEnd)

        lineEnd != null -> lineEnd
        syllableEnd != null && syllableEnd > timeMs ->
            syllableEnd

        else -> null
    }
}

private fun LyricLine.isMarker(): Boolean {
    val value = text.trim()

    return (value.isEmpty() ||
        value.all {
            it == '♪' || it == '♫' ||
                it == '…' || it == '.'
        }) &&
        syllables.none {
            it.text.any(Char::isLetterOrDigit)
        }
}

internal fun buildLyricsStage(
    lines: List<LyricLine>
): LyricsStage {
    if (lines.isEmpty()) return LyricsStage.Empty

    data class Draft(
        val lead: StagedVocal?,
        val backgrounds: MutableList<StagedVocal>,
        val role: VocalRole,
        val start: Long,
        var end: Long?,
        val marker: Long?
    )

    val drafts = mutableListOf<Draft>()
    var pendingMarker: Long? = null

    lines.forEachIndexed { index, line ->

        if (line.isMarker()) {
            if (pendingMarker == null) {
                pendingMarker = line.timeMs
            }

            drafts.lastOrNull()?.let {
                if (it.end == null || it.end!! > line.timeMs) {
                    it.end = maxOf(it.start, line.timeMs)
                }
            }
            return@forEachIndexed
        }

        val leadUnits = line.syllables.filter {
            !it.isBackground
        }

        val backgroundUnits = line.syllables.filter {
            it.isBackground
        }

        val backingOnly =
            line.role == VocalRole.BACKGROUND_SUBLINE ||
                (line.hasSyllables && leadUnits.isEmpty())

        val end = line.knownEnd()

        val lead = if (backingOnly) null else {
            createVocal(
                index,
                line.timeMs,
                end,
                line.text,
                leadUnits
            )
        }

        val background = if (
            backgroundUnits.isNotEmpty() || backingOnly
        ) {
            createVocal(
                index,
                backgroundUnits.firstOrNull()?.timeMs
                    ?: line.timeMs,
                backgroundUnits.maxOfOrNull {
                    it.timeMs + it.durationMs
                } ?: end,
                if (backingOnly) line.text
                    else backgroundUnits.joinToString(" ") { it.text },
                backgroundUnits
            )
        } else null

        val previous = drafts.lastOrNull()

        if (
            backingOnly &&
            background != null &&
            previous != null &&
            pendingMarker == null &&
            line.timeMs <=
                (previous.end ?: previous.start) + BACKING_ATTACH_MS
        ) {
            previous.backgrounds.add(background)
            background.endMs?.let {
                previous.end = maxOf(previous.end ?: it, it)
            }
            return@forEachIndexed
        }

        if (lead == null && background == null) {
            return@forEachIndexed
        }

        drafts.add(
            Draft(
                lead = lead,
                backgrounds = mutableListOfNotNull(background),
                role = if (backingOnly) {
                    previous?.role ?: VocalRole.LEAD_LEFT
                } else line.role,
                start = line.timeMs,
                end = listOfNotNull(
                    end,
                    background?.endMs
                ).maxOrNull(),
                marker = pendingMarker
            )
        )

        pendingMarker = null
    }

    if (drafts.isEmpty()) return LyricsStage.Empty

    val roles = drafts.map { it.role }.toSet()
    val isStaged = roles.size > 1

    val items = mutableListOf<LyricsItem>()

    fun addInterlude(
        start: Long,
        end: Long,
        role: VocalRole
    ) {
        if (end - start >= INTERLUDE_MIN_MS) {
            items.add(
                LyricInterlude(
                    startMs = start,
                    endMs = end,
                    role = if (isStaged) role
                        else VocalRole.LEAD_LEFT
                )
            )
        }
    }

    val first = drafts.first()
    addInterlude(0L, first.start, first.role)

    drafts.forEachIndexed { index, draft ->
        val next = drafts.getOrNull(index + 1)

        var stanzaBreak = false
        var interludeStart: Long? = null

        if (next != null) {
            if (next.marker != null) {
                stanzaBreak = true
            } else {
                val end = draft.end

                if (end != null) {
                    val gap = next.start - end

                    stanzaBreak = gap >= STANZA_GAP_MS

                    if (gap >= INTERLUDE_MIN_MS) {
                        interludeStart = end
                    }
                } else if (
                    next.start - draft.start >= 6000L
                ) {
                    stanzaBreak = true
                }
            }
        }

        items.add(
            LyricLineGroup(
                lead = draft.lead,
                backgrounds = draft.backgrounds.toList(),
                role = draft.role,
                startMs = draft.start,
                endMs = draft.end ?: draft.start,
                endKnown = draft.end != null,
                stanzaGapAfter = stanzaBreak
            )
        )

        if (next != null && interludeStart != null) {
            addInterlude(
                interludeStart,
                next.start,
                next.role
            )
        }
    }

    val starts = LongArray(items.size) {
        items[it].startMs
    }

    return LyricsStage(
        items = items,
        starts = starts,
        isStaged = isStaged
    )
}

private fun createVocal(
    index: Int,
    start: Long,
    end: Long?,
    text: String,
    units: List<LyricSyllable>
): StagedVocal? {
    val cleanUnits = units.filter {
        it.text.isNotBlank()
    }

    val displayText = if (cleanUnits.isNotEmpty()) {
        cleanUnits.joinToString("") { it.text }
    } else {
        text.replace(Regex("\\s+"), " ").trim()
    }

    if (displayText.isBlank()) return null

    return StagedVocal(
        lineIndex = index,
        startMs = start,
        endMs = end,
        text = displayText,
        units = cleanUnits,
        isRtl = isRtlText(displayText)
    )
}
