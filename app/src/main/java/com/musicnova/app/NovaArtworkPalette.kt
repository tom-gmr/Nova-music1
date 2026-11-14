package com.musicnova.app

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Artwork colours used by MusicNova's dynamic player background.
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

    if (artworkUrl.isBlank()) return@withContext null

    try {
        val request = ImageRequest.Builder(context)
            .data(artworkUrl)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)

        val bitmap = (result as? SuccessResult)
            ?.drawable
            ?.let { drawable ->
                when (drawable) {
                    is BitmapDrawable -> drawable.bitmap
                    else -> null
                }
            } ?: return@withContext null

        val palette = Palette.from(bitmap)
            .clearFilters()
            .maximumColorCount(24)
            .generate()

        val swatches = palette.swatches
            .sortedByDescending { it.population }

        if (swatches.isEmpty()) return@withContext null

        fun getHsl(rgb: Int): FloatArray {
            return FloatArray(3).also {
                ColorUtils.colorToHSL(rgb, it)
            }
        }

        fun hueDistance(first: Float, second: Float): Float {
            val difference = abs(first - second) % 360f
            return minOf(difference, 360f - difference)
        }

        val primarySwatch = swatches.first()
        val primaryRgb = primarySwatch.rgb
        val primaryHsl = getHsl(primaryRgb)

        val secondarySwatch = swatches.firstOrNull { swatch ->
            val hsl = getHsl(swatch.rgb)

            hsl[1] >= 0.18f &&
                hueDistance(hsl[0], primaryHsl[0]) >= 28f
        } ?: swatches.getOrNull(1) ?: primarySwatch

        val secondaryRgb = secondarySwatch.rgb
        val secondaryHsl = getHsl(secondaryRgb)

        val tertiarySwatch = swatches.firstOrNull { swatch ->
            val hsl = getHsl(swatch.rgb)

            hsl[1] >= 0.16f &&
                hueDistance(hsl[0], primaryHsl[0]) >= 48f &&
                hueDistance(hsl[0], secondaryHsl[0]) >= 24f
        } ?: swatches.lastOrNull() ?: primarySwatch

        NovaArtworkPalette(
            primary = Color(primaryRgb),
            secondary = Color(secondaryRgb),
            tertiary = Color(tertiarySwatch.rgb)
        )

    } catch (_: Exception) {
        null
    }
}
