package com.musicnova.app.ui.feed.cards

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CardDimensions(
    val screenWidth: Dp,
    val coverHeight: Dp,
    val squareWidth: Dp,
    val wideWidth: Dp,
    val totalHeight: Dp,
) {
    val totalSquareCardHeight: Dp get() = totalHeight
    val totalWideCardHeight: Dp get() = totalHeight
}

object CardTokens {

    const val COVER_HEIGHT_RATIO = 0.42f

    val MIN_COVER_HEIGHT: Dp = 132.dp
    val MAX_COVER_HEIGHT: Dp = 200.dp

    val coverCornerRadius: Dp = 4.dp
    val cardGap: Dp = 12.dp
    val sectionGap: Dp = 16.dp
    val coverToTextGap: Dp = 8.dp
    val textAreaHeight: Dp = 42.dp
    val titleToInfoGap: Dp = 2.dp

    val titleFontSize: TextUnit = 14.sp
    val titleLineHeight: TextUnit = 20.sp
    val infoFontSize: TextUnit = 12.sp
    val infoLineHeight: TextUnit = 16.sp
}

fun calculateCardDimensions(
    screenWidth: Dp,
    coverHeightRatio: Float = CardTokens.COVER_HEIGHT_RATIO,
    minCoverHeight: Dp = CardTokens.MIN_COVER_HEIGHT,
    maxCoverHeight: Dp = CardTokens.MAX_COVER_HEIGHT,
    coverToTextGap: Dp = CardTokens.coverToTextGap,
    textAreaHeight: Dp = CardTokens.textAreaHeight,
): CardDimensions {

    val rawHeight = screenWidth * coverHeightRatio
    val coverHeight = rawHeight.coerceIn(
        minCoverHeight,
        maxCoverHeight
    )

    val squareWidth = coverHeight
    val wideWidth = (coverHeight.value * (16f / 9f)).dp
    val totalHeight = coverHeight + coverToTextGap + textAreaHeight

    return CardDimensions(
        screenWidth = screenWidth,
        coverHeight = coverHeight,
        squareWidth = squareWidth,
        wideWidth = wideWidth,
        totalHeight = totalHeight
    )
}
