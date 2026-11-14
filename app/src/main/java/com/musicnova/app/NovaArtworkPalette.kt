
package com.musicnova.app

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Artwork ambient colours selected using LastWave's
 * FullPlayer palette selection mechanism.
 */
data class NovaArtworkPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

suspend fun extractNovaArtworkPalette(
    context: Context,
    artworkUrl: String
): NovaArtworkPalette? = withContext(Dispatchers.IO) {

    if (artworkUrl.isBlank()) {
        return@withContext null
    }

    try {
        val request = ImageRequest.Builder(context)
            .data(artworkUrl)
            .allowHardware(false)
            .size(128)
            .build()

        val result = context.imageLoader.execute(request)

        val bitmap = (
            (result as? SuccessResult)?.drawable as? BitmapDrawable
        )?.bitmap ?: return@withContext null

        val palette = Palette.from(bitmap)
            .clearFilters()
            .generate()

        // Same swatch priority as LastWave FullPlayer.kt.
        val primarySwatch =
            palette.vibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
                ?: return@withContext null

        val secondarySwatch =
            palette.lightVibrantSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.mutedSwatch
                ?: palette.dominantSwatch
                ?: primarySwatch

        val tertiarySwatch =
            palette.darkVibrantSwatch
                ?: palette.darkMutedSwatch
                ?: palette.dominantSwatch
                ?: primarySwatch

        NovaArtworkPalette(
            primary = Color(primarySwatch.rgb),
            secondary = Color(secondarySwatch.rgb),
            tertiary = Color(tertiarySwatch.rgb)
        )

    } catch (_: Exception) {
        null
    }
}
