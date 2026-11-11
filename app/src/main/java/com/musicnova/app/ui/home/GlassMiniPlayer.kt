package com.musicnova.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val PillShape = RoundedCornerShape(44.dp)

private class SongHolder { var song: HomeSong? = null }

@Composable
fun GlassMiniPlayer(
    state: PlayerUiState,
    actions: PlayerActions,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val holder = remember { SongHolder() }
    state.song?.let { holder.song = it }

    AnimatedVisibility(
        visible = state.song != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 300f)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        val song = holder.song ?: return@AnimatedVisibility
        PillContent(song, state, actions, hazeState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PillContent(
    song: HomeSong,
    state: PlayerUiState,
    actions: PlayerActions,
    hazeState: HazeState
) {
    val currentActions by rememberUpdatedState(actions)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { 80.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var menuOpen by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(400, easing = LinearEasing),
        label = "progress"
    )

    Box(
        Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .liquidGlass(hazeState, PillShape)
                .combinedClickable(
                    onClick = { currentActions.onOpenPlayer() },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    }
                )
                .pointerInput(Unit) {
                    var axis = 0
                    fun finish() {
                        val dx = offsetX.value
                        val dy = offsetY.value
                        scope.launch {
                            when {
                                dx < -threshold -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentActions.onNext()
                                }
                                dx > threshold -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentActions.onPrevious()
                                }
                                dy > threshold -> currentActions.onDismiss()
                            }
                            launch { offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f)) }
                            launch { offsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f)) }
                        }
                    }
                    detectDragGestures(
                        onDragStart = { axis = 0 },
                        onDragEnd = { finish() },
                        onDragCancel = { finish() },
                        onDrag = { change, drag ->
                            change.consume()
                            if (axis == 0) axis = if (abs(drag.x) > abs(drag.y)) 1 else 2
                            scope.launch {
                                if (axis == 1) {
                                    offsetX.snapTo(offsetX.value + drag.x * 0.7f)
                                } else {
                                    offsetY.snapTo((offsetY.value + drag.y * 0.7f).coerceAtLeast(0f))
                                }
                            }
                        }
                    )
                }
                .padding(start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = song,
                contentKey = { it.id },
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                modifier = Modifier.weight(1f),
                label = "song"
            ) { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Cover(
                        s.artworkUrl,
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(HomeColors.CardHigh),
                        px = 300
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Text(
                            s.artist.ifBlank { "Unknown artist" },
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    drawArc(
                        Color.White.copy(alpha = 0.16f), -90f, 360f, false,
                        topLeft, arcSize, style = Stroke(stroke)
                    )
                    drawArc(
                        HomeColors.Accent, -90f, 360f * progress, false,
                        topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                GlassCircleButton(
                    onClick = { currentActions.onTogglePlay() },
                    size = 46.dp,
                    container = HomeColors.Accent,
                    label = if (state.isPlaying) "Pause" else "Play"
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = HomeColors.OnAccent
                        )
                    } else {
                        Crossfade(targetState = state.isPlaying, label = "playPause") { playing ->
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = HomeColors.OnAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            GlassCircleButton(
                onClick = { currentActions.onNext() },
                size = 46.dp,
                container = Color.White.copy(alpha = 0.12f),
                label = "Next"
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (song.isLiked) "Unlike" else "Like") },
                leadingIcon = {
                    Icon(if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null)
                },
                onClick = { menuOpen = false; currentActions.onToggleLike() }
            )
            DropdownMenuItem(
                text = { Text("Previous") },
                leadingIcon = { Icon(Icons.Rounded.SkipPrevious, null) },
                onClick = { menuOpen = false; currentActions.onPrevious() }
            )
            DropdownMenuItem(
                text = { Text("Stop and hide") },
                leadingIcon = { Icon(Icons.Rounded.Close, null) },
                onClick = { menuOpen = false; currentActions.onDismiss() }
            )
        }
    }
}
