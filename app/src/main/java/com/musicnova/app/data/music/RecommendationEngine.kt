package com.musicnova.app.data.music

/**
 * Local signals used to rank recommendation candidates.
 * This class does not control playback or fetch songs.
 */
data class RecommendationSignals(
    val likedVideoIds: Set<String> = emptySet(),
    val recentlyPlayedVideoIds: Set<String> = emptySet(),
    val excludedVideoIds: Set<String> = emptySet(),
    val artistAffinity: Map<String, Float> = emptyMap(),
    val listenedDurationMs: Map<String, Long> = emptyMap(),
    val skipCounts: Map<String, Int> = emptyMap(),
    val lastPlayedAtMs: Map<String, Long> = emptyMap(),
    val timeOfDayAffinity: Map<String, Float> = emptyMap(),
    val nowMs: Long = System.currentTimeMillis()
)

/**
 * Local-first recommendation ranking.
 */
object RecommendationEngine {

    fun rank(
        candidates: List<YouTubeMusicTrack>,
        signals: RecommendationSignals,
        limit: Int = 30
    ): List<YouTubeMusicTrack> {
        if (limit <= 0) return emptyList()

        val unique = LinkedHashMap<String, YouTubeMusicTrack>()

        candidates.forEach { track ->
            if (track.isArtist || track.videoId.isBlank()) {
                return@forEach
            }

            if (track.videoId in signals.excludedVideoIds) {
                return@forEach
            }

            unique.putIfAbsent(track.videoId, track)
        }

        return unique.values
            .sortedWith(
                compareByDescending<YouTubeMusicTrack> {
                    score(it, signals)
                }.thenBy {
                    it.title.lowercase()
                }
            )
            .take(limit)
    }
    fun score(
        track: YouTubeMusicTrack,
        signals: RecommendationSignals
    ): Float {
        val id = track.videoId
        var score = 0f

        if (id in signals.likedVideoIds) {
            score += 8f
        }

        if (id in signals.recentlyPlayedVideoIds) {
            score -= 5f
        }

        if (track.isOfficial) {
            score += 1.5f
        }

        score += signals.artistAffinity[track.artist]
            ?.coerceIn(-5f, 5f) ?: 0f

        val listenedMinutes =
            (signals.listenedDurationMs[id] ?: 0L) / 60_000f

        score += (listenedMinutes / 10f).coerceIn(0f, 3f)

        val skips = (signals.skipCounts[id] ?: 0)
            .coerceAtLeast(0)

        score -= (skips * 1.5f).coerceAtMost(6f)

        val lastPlayed = signals.lastPlayedAtMs[id]

        if (lastPlayed != null && lastPlayed > 0L) {
            val ageHours =
                (signals.nowMs - lastPlayed)
                    .coerceAtLeast(0L) / 3_600_000f

            val recencyPenalty =
                (6f - ageHours / 12f).coerceAtLeast(0f)

            score -= recencyPenalty
        }

        score += signals.timeOfDayAffinity[id]
            ?.coerceIn(-2f, 2f) ?: 0f

        return score
    }
}
