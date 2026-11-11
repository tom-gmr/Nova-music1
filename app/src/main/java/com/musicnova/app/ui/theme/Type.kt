
@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.musicnova.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.musicnova.app.R

private fun gsFlex(
    weight: Float,
    width: Float = 100f,
    round: Float = 0f,
    opticalSize: Float
): FontFamily = FontFamily(
    Font(
        R.font.google_sans_flex,
        weight = when {
            weight >= 700f -> FontWeight.Bold
            weight >= 600f -> FontWeight.SemiBold
            weight >= 500f -> FontWeight.Medium
            else -> FontWeight.Normal
        },
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.toInt()),
            FontVariation.width(width),
            FontVariation.Setting("ROND", round),
            FontVariation.Setting("opsz", opticalSize)
        )
    )
)

val LastWaveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = gsFlex(760f, 122f, 42f, 57f),
        fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = gsFlex(740f, 120f, 40f, 45f),
        fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = gsFlex(720f, 118f, 38f, 36f),
        fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = gsFlex(800f, 122f, 40f, 32f),
        fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = gsFlex(780f, 120f, 38f, 28f),
        fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = gsFlex(760f, 116f, 36f, 24f),
        fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = gsFlex(640f, 110f, 30f, 22f),
        fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = gsFlex(600f, 106f, 28f, 16f),
        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = gsFlex(580f, 105f, 26f, 14f),
        fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = gsFlex(440f, 102f, 18f, 16f),
        fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = gsFlex(430f, 102f, 18f, 15f),
        fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = gsFlex(420f, 100f, 16f, 13f),
        fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = gsFlex(680f, 108f, 46f, 14f),
        fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = gsFlex(660f, 108f, 44f, 12f),
        fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = gsFlex(660f, 108f, 44f, 11f),
        fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp
    )
)
