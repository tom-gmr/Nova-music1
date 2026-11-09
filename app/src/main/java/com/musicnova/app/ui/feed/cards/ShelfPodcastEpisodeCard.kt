package com.musicnova.app.ui.feed.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShelfPodcastEpisodeCard(
    title: String,
    infoLine: String,
    artworkUrl: String?,
    durationText: String?,
    progressPercentage: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    cardWidth: Dp = rememberPodcastEpisodeCardWidth(),
) {
    val coverHeight = (cardWidth.value * (9f / 16f)).dp

    Column(
        modifier = modifier
            .width(cardWidth)
            .cardPressable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(coverHeight)
                .clip(RoundedCornerShape(CardTokens.coverCornerRadius))
        ) {
            CardArtwork(
                title = title,
                subtitle = infoLine,
                artworkUrl = artworkUrl,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = CardTokens.coverCornerRadius
            )

            if (!durationText.isNullOrBlank()) {
                val hasProgress =
                    progressPercentage != null && progressPercentage > 0f

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 8.dp,
                            bottom = if (hasProgress) 10.dp else 8.dp
                        )
                ) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(
                            horizontal = 6.dp,
                            vertical = 2.dp
                        )
                    )
                }
            }

            if (progressPercentage != null && progressPercentage > 0f) {
                val clamped = progressPercentage.coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(clamped)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(Modifier.height(CardTokens.coverToTextGap))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardTokens.textAreaHeight)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = CardTokens.titleFontSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = CardTokens.titleLineHeight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                if (infoLine.isNotBlank()) {
                    Spacer(Modifier.height(CardTokens.titleToInfoGap))

                    Text(
                        text = infoLine,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = CardTokens.infoFontSize,
                            lineHeight = CardTokens.infoLineHeight
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (onMenuClick != null) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
