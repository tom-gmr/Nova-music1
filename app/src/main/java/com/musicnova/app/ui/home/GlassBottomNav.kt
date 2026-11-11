package com.musicnova.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState

@Composable
fun GlassBottomNav(
    items: List<NavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    onReselect: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val selectedIndex = items.indexOfFirst {
        it.route == selectedRoute
    }.coerceAtLeast(0)

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 4.dp)
            .liquidGlass(
                hazeState,
                shape,
                tintAlpha = 0.58f
            )
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            val itemWidth = maxWidth / items.size
            val indicatorWidth = (itemWidth - 12.dp)
                .coerceAtMost(82.dp)

            val indicatorX by animateDpAsState(
                targetValue = itemWidth * selectedIndex +
                    (itemWidth - indicatorWidth) / 2,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 380f
                ),
                label = "indicatorX"
            )

            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(indicatorWidth)
                    .height(48.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        Color(0xFFAA8BFF).copy(alpha = 0.18f)
                    )
            )

            Row(Modifier.fillMaxWidth()) {
                items.forEach { item ->
                    NavButton(
                        item = item,
                        selected = item.route == selectedRoute,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (item.route == selectedRoute) {
                                onReselect(item.route)
                            } else {
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove
                                )
                                onSelect(item.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavButton(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (selected) {
            Color(0xFFD9C9FF)
        } else {
            Color.White.copy(alpha = 0.62f)
        },
        label = "navColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "navScale"
    )

    Column(
        modifier
            .height(48.dp)
            .selectable(
                selected = selected,
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = color,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )

            if (item.badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .defaultMinSize(
                            minWidth = 16.dp,
                            minHeight = 16.dp
                        )
                        .clip(CircleShape)
                        .background(Color(0xFFFF5A6E))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (item.badge > 99) {
                            "99+"
                        } else {
                            item.badge.toString()
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            item.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Medium
            },
            maxLines = 1
        )
    }
}
