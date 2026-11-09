package com.musicnova.app.ui.feed.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberCardDimensions(): CardDimensions {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return remember(screenWidth) {
        calculateCardDimensions(screenWidth)
    }
}

@Composable
fun rememberCoverHeight(): Dp =
    rememberCardDimensions().coverHeight

@Composable
fun rememberSquareCardWidth(): Dp {
    val dimensions = rememberCardDimensions()
    val peekWidth = with(LocalDensity.current) { 20.toDp() }

    val widthForTwoCards =
        (dimensions.screenWidth - 48.dp - CardTokens.cardGap * 2 - peekWidth) / 2

    return dimensions.squareWidth.coerceAtMost(widthForTwoCards)
}

@Composable
fun rememberWideCardWidth(): Dp =
    rememberCardDimensions().wideWidth

@Composable
fun rememberFeaturedCardWidth(): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    return when {
        screenWidth < 360.dp -> (screenWidth * 0.56f).coerceAtLeast(180.dp)
        screenWidth < 600.dp -> (screenWidth * 0.54f).coerceAtMost(260.dp)
        screenWidth < 840.dp -> (screenWidth * 0.36f).coerceAtMost(300.dp)
        else -> (screenWidth * 0.24f).coerceAtMost(320.dp)
    }
}

@Composable
fun rememberCommunityCardWidth(): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    return when {
        screenWidth < 360.dp -> (screenWidth * 0.78f).coerceAtLeast(260.dp)
        screenWidth < 600.dp -> (screenWidth * 0.76f).coerceAtMost(340.dp)
        screenWidth < 840.dp -> (screenWidth * 0.5f).coerceAtMost(380.dp)
        else -> (screenWidth * 0.34f).coerceAtMost(400.dp)
    }
}

@Composable
fun rememberPodcastEpisodeCardWidth(): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return (screenWidth * 0.80f).coerceIn(260.dp, 400.dp)
}
