package com.musicnova.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Liquid glass: peeche ka content blur + halka dark tint + upar se sheen
 * + chamakti hui patli rim (border).
 */
fun Modifier.liquidGlass(
    state: HazeState,
    shape: Shape,
    tint: Color = Color(0xFF120E1A),
    tintAlpha: Float = 0.38f,
    blur: Dp = 26.dp
): Modifier = this
    .clip(shape)
    .hazeEffect(state = state) {
        blurRadius = blur
        backgroundColor = Color(0xFF0B0812)
        tints = listOf(HazeTint(tint.copy(alpha = tintAlpha)))
        fallbackTint = HazeTint(tint.copy(alpha = 0.85f))
        noiseFactor = 0.04f
    }
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.55f),
            0.45f to Color.White.copy(alpha = 0.10f),
            1f to Color.White.copy(alpha = 0.32f)
        ),
        shape = shape
    )

@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    size: Dp,
    container: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "btnScale"
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(container)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { if (label != null) this.contentDescription = label },
        contentAlignment = Alignment.Center,
        content = content
    )
}
