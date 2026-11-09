package com.musicnova.app.ui.feed.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.musicnova.app.data.music.YouTubeMusicTrack
import androidx.compose.ui.graphics.compositeOver

@Composable
fun ShelfCommunityPlaylistCard(
    title: String,
    subtitle: String,
    collageUrls: List<String>,
    songs: List<YouTubeMusicTrack>,
    artworkUrl: String?,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onSongClick: (YouTubeMusicTrack) -> Unit,
    modifier: Modifier = Modifier,
    onSongMenuClick: ((YouTubeMusicTrack) -> Unit)? = null,
    cardWidth: Dp = rememberCommunityCardWidth(),
    isLoading: Boolean = false,
    previewFailed: Boolean = false,
    songPlayCounts: Map<String, String> = emptyMap(),
    playlistId: String? = null
) {
    val fontScale = androidx.compose.ui.platform.LocalDensity.current
        .fontScale.coerceAtLeast(1f)

    val rowHeight = 56.dp * fontScale
    val defaultSurface = MaterialTheme.colorScheme.surfaceContainerHigh
    val background by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary
            .copy(alpha = 0.10f)
            .let { tint -> tint.compositeOver(defaultSurface) },
        animationSpec = tween(300),
        label = "communityPlaylistTint"
    )

    Surface(
        modifier = modifier.width(cardWidth),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardPressable(onClick = onClick)
                    .semantics {
                        contentDescription = "Open playlist $title"
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CommunityCollageCover(
                    urls = collageUrls,
                    fallback = artworkUrl,
                    modifier = Modifier.size(cardWidth * 0.37f)
                )
                Spacer(Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp * fontScale),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .cardPressable(onClick = onClick),
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.width(8.dp))

                Surface(
                    onClick = onPlayClick,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Play playlist $title",
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp * fontScale)
                    .cardPressable(onClick = onClick),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight * 3 + 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when {
                    isLoading -> repeat(3) {
                        CommunityLoadingSongRow(rowHeight)
                    }

                    !previewFailed -> repeat(3) { index ->
                        val song = songs.getOrNull(index)

                        if (song == null) {
                            Spacer(Modifier.height(rowHeight))
                        } else {
                            CommunitySongRow(
                                song = song,
                                plays = songPlayCounts[song.videoId],
                                height = rowHeight,
                                onClick = { onSongClick(song) },
                                onMenuClick = onSongMenuClick?.let {
                                    { it(song) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityCollageCover(
    urls: List<String>,
    fallback: String?,
    modifier: Modifier = Modifier
) {
    val covers = urls
        .filter(String::isNotBlank)
        .distinct()
        .take(4)

    if (covers.isEmpty()) {
        CommunityPreviewArtwork(
            url = fallback,
            fallbackIcon = Icons.Filled.QueueMusic,
            modifier = modifier
        )
        return
    }

    val tiles = List(4) { index ->
        covers.getOrNull(index) ?: covers.first()
    }

    Column(
        modifier = modifier.clip(
            RoundedCornerShape(CardTokens.coverCornerRadius)
        )
    ) {
        repeat(2) { row ->
            Row(Modifier.weight(1f)) {
                repeat(2) { column ->
                    CommunityPreviewArtwork(
                        url = tiles[row * 2 + column],
                        fallbackIcon = Icons.Filled.QueueMusic,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityPreviewArtwork(
    url: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = fallbackIcon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.4f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CommunitySongRow(
    song: YouTubeMusicTrack,
    plays: String?,
    height: Dp,
    onClick: () -> Unit,
    onMenuClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(CardTokens.coverCornerRadius))
            .cardPressable(onClick = onClick)
            .semantics {
                contentDescription = "Play ${song.title}, ${song.artist}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        CommunityPreviewArtwork(
            url = song.artworkUrl,
            fallbackIcon = Icons.Filled.MusicNote,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(CardTokens.coverCornerRadius))
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = listOfNotNull(
                    song.artist.takeIf(String::isNotBlank),
                    plays
                ).joinToString(" • "),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onMenuClick != null) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More options for ${song.title}",
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CommunityLoadingSongRow(height: Dp) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(base)
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(13.dp)
                    .clip(CircleShape)
                    .background(base)
            )

            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(11.dp)
                    .clip(CircleShape)
                    .background(base)
            )
        }
    }
}
