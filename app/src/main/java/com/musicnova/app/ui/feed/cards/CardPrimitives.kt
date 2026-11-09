package com.musicnova.app.ui.feed.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.cardPressable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "cardPressScale"
    )

    return this
        .scale(scale)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = {
                haptics.performHapticFeedback(
                    HapticFeedbackType.TextHandleMove
                )
                onClick()
            },
            onLongClick = onLongClick?.let {
                {
                    haptics.performHapticFeedback(
                        HapticFeedbackType.LongPress
                    )
                    it()
                }
            }
        )
}

@Composable
internal fun CardArtwork(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CardTokens.coverCornerRadius,
) {
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            if (artworkUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "$title by $subtitle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "$title by $subtitle",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
internal fun CardTextArea(
    title: String,
    infoLine: String? = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(CardTokens.textAreaHeight)
            .padding(horizontal = 2.dp),
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

        if (!infoLine.isNullOrBlank()) {
            Spacer(Modifier.height(CardTokens.titleToInfoGap))

            Text(
                text = infoLine,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = CardTokens.infoFontSize,
                    lineHeight = CardTokens.infoLineHeight
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.75f
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun CardTitle(
    text: String,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
internal fun CardInfoLine(
    text: String,
    modifier: Modifier = Modifier
) {
    if (text.isBlank()) return

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.8f
        ),
        modifier = modifier
    )
}
