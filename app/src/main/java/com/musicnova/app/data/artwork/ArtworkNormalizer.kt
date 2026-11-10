package com.musicnova.app.data.artwork

object ArtworkNormalizer {

    private const val LASTFM_NO_ART_HASH =
        "2a96cbd8b46e442fc41c2b86b821562f"

    fun isRealImage(url: String?): Boolean {
        return !url.isNullOrBlank() &&
            !url.contains(LASTFM_NO_ART_HASH)
    }

    fun cacheKey(name: String, artist: String): String {
        return "t:${name.trim()}|${artist.trim()}".lowercase()
    }

    private val youtubeSizeRegex =
        Regex("""=w\d+-h\d+[^?#]*|=s\d+[^?#]*""")

    fun upscaleYoutubeArtwork(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (!isRealImage(url)) return null

        var result = url

        if (
            result.contains("googleusercontent.com") ||
            result.contains("ggpht.com")
        ) {
            result = if (youtubeSizeRegex.containsMatchIn(result)) {
                result.replace(
                    youtubeSizeRegex,
                    "=w1200-h1200-l90-rj"
                )
            } else if ('=' in result) {
                result.substringBeforeLast('=') +
                    "=w1200-h1200-l90-rj"
            } else {
                "$result=w1200-h1200-l90-rj"
            }
        }

        return result
    }
}
