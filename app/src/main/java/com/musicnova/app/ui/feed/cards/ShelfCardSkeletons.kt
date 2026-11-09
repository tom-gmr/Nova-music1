
package com.musicnova.app.ui.feed.cards

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun rememberShimmerPulse(): Float {
    val transition = rememberInfiniteTransition(label = "shelfSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shelfSkeletonPulse"
    )
    return pulse
}

private fun Modifier.shimmer(pulse: Float, radius: Dp): Modifier =
    clip(RoundedCornerShape(radius))
        .background(
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = 0.7f * pulse + 0.3f
            )
        )

@Composable
fun ShelfSquareSkeletonRow(
    cardWidth: Dp = rememberSquareCardWidth(),
    count: Int = 4
) {
    val pulse = rememberShimmerPulse()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(CardTokens.cardGap),
        userScrollEnabled = false
    ) {
        items(count) {
            Column {
                Box(
                    Modifier.width(cardWidth)
                        .height(cardWidth)
                        .shimmer(pulse, CardTokens.coverCornerRadius)
                )
                Spacer(Modifier.height(CardTokens.coverToTextGap))
                Box(
                    Modifier.width(cardWidth * 0.7f)
                        .height(12.dp)
                        .shimmer(pulse, 4.dp)
                )
                Spacer(Modifier.height(CardTokens.titleToInfoGap))
                Box(
                    Modifier.width(cardWidth * 0.5f)
                        .height(10.dp)
                        .shimmer(pulse, 4.dp)
                )
            }
        }
    }
}

@Composable
fun ShelfWideSkeletonRow(
    cardWidth: Dp = rememberWideCardWidth(),
    count: Int = 3
) {
    val pulse = rememberShimmerPulse()
    val coverHeight = rememberCoverHeight()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(CardTokens.cardGap),
        userScrollEnabled = false
    ) {
        items(count) {
            Column {
                Box(
                    Modifier.width(cardWidth)
                        .height(coverHeight)
                        .shimmer(pulse, CardTokens.coverCornerRadius)
                )
                Spacer(Modifier.height(CardTokens.coverToTextGap))
                Box(
                    Modifier.width(cardWidth * 0.7f)
                        .height(12.dp)
                        .shimmer(pulse, 4.dp)
                )
                Spacer(Modifier.height(CardTokens.titleToInfoGap))
                Box(
                    Modifier.width(cardWidth * 0.5f)
                        .height(10.dp)
                        .shimmer(pulse, 4.dp)
                )
            }
        }
    }
}
