package com.musicnova.app.ui.feed.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

@Composable
fun ShelfWideCard(
    title: String,
    infoLine: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    fallbackIcon: ImageVector = Icons.Filled.PlayCircle,
    cardWidth: Dp = rememberWideCardWidth(),
) {
    val coverHeight = rememberCoverHeight()

    Column(
        modifier = modifier
            .width(cardWidth)
            .cardPressable(onClick, onLongClick)
    ) {
        Box(
            Modifier.width(cardWidth).height(coverHeight)
        ) {
            CardArtwork(
                title = title,
                subtitle = infoLine,
                artworkUrl = artworkUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = CardTokens.coverCornerRadius
            )
        }
        Spacer(Modifier.height(CardTokens.coverToTextGap))
        CardTextArea(title = title, infoLine = infoLine)
    }
}
