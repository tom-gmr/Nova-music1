
package com.musicnova.app.ui.feed.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PodcastHeroBackground(
    backgroundUrl: String?,
    scrollOffsetY: Float,
    modifier: Modifier = Modifier,
    heroHeight: Dp = 320.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .graphicsLayer {
                translationY = -scrollOffsetY.coerceAtLeast(0f)
            }
    ) {
        AsyncImage(
            model = backgroundUrl?.takeIf { it.isNotBlank() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val bgColor = MaterialTheme.colorScheme.background

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.60f),
                        0.30f to Color.Black.copy(alpha = 0.35f),
                        0.60f to Color.Black.copy(alpha = 0.50f),
                        0.85f to bgColor.copy(alpha = 0.85f),
                        1.0f to bgColor
                    )
                )
        )
    }
}
