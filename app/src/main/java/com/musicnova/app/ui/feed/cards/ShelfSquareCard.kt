
package com.musicnova.app.ui.feed.cards

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun ShelfSquareCard(
    title: String,
    infoLine: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    cardWidth: Dp = rememberSquareCardWidth(),
) {
    Column(
        modifier = modifier
            .width(cardWidth)
            .cardPressable(onClick, onLongClick)
    ) {
        Box(
            Modifier.width(cardWidth).height(cardWidth)
        ) {
            CardArtwork(
                title = title,
                subtitle = infoLine,
                artworkUrl = artworkUrl,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(CardTokens.coverToTextGap))
        CardTextArea(title = title, infoLine = infoLine)
    }
}
