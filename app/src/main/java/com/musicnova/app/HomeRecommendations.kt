package com.musicnova.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Calendar

/* ---------------------------------------------------------------
 *  MODELS  (apne player/repository ke data ko in mein map karo)
 * --------------------------------------------------------------- */

data class HomeSong(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String = "",
    val artworkUrl: String? = null,
    val isLiked: Boolean = false
)

data class HomePlaylist(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val artworkUrls: List<String> = emptyList()
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val featured: HomeSong? = null,
    val recent: List<HomeSong> = emptyList(),
    val quickPicks: List<HomeSong> = emptyList(),
    val madeForYou: List<HomeSong> = emptyList(),
    val trending: List<HomeSong> = emptyList(),
    val communityPlaylists: List<HomePlaylist> = emptyList(),
    val likedCount: Int = 0,
    val downloadCount: Int = 0,
    val playlistCount: Int = 0
)

class HomeActions(
    val onPlaySong: (HomeSong) -> Unit = {},
    val onPlayNext: (HomeSong) -> Unit = {},
    val onAddToQueue: (HomeSong) -> Unit = {},
    val onToggleLike: (HomeSong) -> Unit = {},
    val onPlaylistClick: (HomePlaylist) -> Unit = {},
    val onShuffleQuickPicks: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onOpenLiked: () -> Unit = {},
    val onOpenRecent: () -> Unit = {},
    val onOpenDownloads: () -> Unit = {},
    val onOpenPlaylists: () -> Unit = {},
    val onMoodSelected: (String?) -> Unit = {}
)

/* ---------------------------------------------------------------
 *  COLORS
 * --------------------------------------------------------------- */

internal object HomeColors {
    val Bg = Color(0xFF07050C)
    val Card = Color(0xFF16131F)
    val CardHigh = Color(0xFF221D30)
    val Accent = Color(0xFFD0BCFF)
    val AccentDeep = Color(0xFF6F4FB8)
    val OnAccent = Color(0xFF1B1030)
    val TextPrimary = Color(0xFFF5F2FA)
    val TextSecondary = Color(0xFFA9A2BA)
}

/* ---------------------------------------------------------------
 *  HOME SCREEN
 * --------------------------------------------------------------- */

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    selectedMood: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val sections = remember(state) { buildSections(state) }
    val playlists = remember(state.communityPlaylists) { state.communityPlaylists.distinctBy { it.id } }
    val nothingToShow = state.featured == null && sections.isEmpty && playlists.isEmpty()

    Box(modifier.fillMaxSize().background(HomeColors.Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            item(key = "header") {
                HomeHeader(refreshing = state.isRefreshing, onRefresh = actions.onRefresh)
            }
            item(key = "moods") {
                MoodChips(selected = selectedMood, onSelect = actions.onMoodSelected)
            }

            when {
                state.isLoading && nothingToShow -> {
                    item(key = "skeleton") { HomeSkeleton() }
                }
                nothingToShow -> {
                    item(key = "empty") { EmptyState(actions.onRefresh) }
                }
                else -> {
                    state.featured?.let { featured ->
                        item(key = "hero") { HeroCard(featured, actions.onPlaySong) }
                    }
                    item(key = "access") { QuickAccessRow(state, actions) }

                    if (sections.recent.isNotEmpty()) {
                        item(key = "recent") {
                            Section("Jump back in", "Continue where you left off") {
                                SongRow(sections.recent, 150.dp, actions)
                            }
                        }
                    }
                    if (sections.quick.isNotEmpty()) {
                        item(key = "quick") {
                            Section(
                                title = "Quick picks",
                                actionLabel = "Shuffle",
                                onAction = actions.onShuffleQuickPicks
                            ) { SongRow(sections.quick, 180.dp, actions) }
                        }
                    }
                    if (sections.forYou.isNotEmpty()) {
                        item(key = "foryou") {
                            Section("Made for you", "Your personal music mix") {
                                SongRow(sections.forYou, 150.dp, actions)
                            }
                        }
                    }
                    if (sections.trending.isNotEmpty()) {
                        item(key = "trending") {
                            Section("Trending now", "Music people are listening to") {
                                SongRow(sections.trending, 150.dp, actions)
                            }
                        }
                    }
                    if (playlists.isNotEmpty()) {
                        item(key = "playlists") {
                            Section("Community playlists") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(playlists, key = { it.id }) { p ->
                                        PlaylistCard(p) { actions.onPlaylistClick(p) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "footer") { HomeFooter() }
        }

        // Bottom fade: floating nav bar ke peeche content saaf dikhe
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, HomeColors.Bg)))
        )
    }
}

/* ---------------------------------------------------------------
 *  DEDUPE: ek song ek screen par sirf ek baar
 * --------------------------------------------------------------- */

private data class Sections(
    val recent: List<HomeSong>,
    val quick: List<HomeSong>,
    val forYou: List<HomeSong>,
    val trending: List<HomeSong>
) {
    val isEmpty: Boolean
        get() = recent.isEmpty() && quick.isEmpty() && forYou.isEmpty() && trending.isEmpty()
}

private fun buildSections(s: HomeUiState): Sections {
    val seen = HashSet<String>()
    s.featured?.let { seen.add(it.id) }
    fun take(list: List<HomeSong>): List<HomeSong> = list.filter { seen.add(it.id) }
    val recent = take(s.recent)
    val quick = take(s.quickPicks)
    val forYou = take(s.madeForYou)
    val trending = take(s.trending)
    return Sections(recent, quick, forYou, trending)
}

/* ---------------------------------------------------------------
 *  HEADER + MOODS
 * --------------------------------------------------------------- */

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Late night vibes"
    }
}

@Composable
private fun HomeHeader(refreshing: Boolean, onRefresh: () -> Unit) {
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing)),
        label = "angle"
    )

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(HomeColors.AccentDeep, Color(0xFF9B7FE0)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "MUSIC NOVA",
                color = HomeColors.Accent,
                fontSize = 11.sp,
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                greeting(),
                color = HomeColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = HomeColors.Accent,
                modifier = Modifier.rotate(if (refreshing) angle else 0f)
            )
        }
    }
}

private val Moods = listOf("Chill", "Romantic", "Party", "Workout", "Focus", "Sad")

@Composable
private fun MoodChips(selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(Moods) { mood ->
            val isSelected = mood == selected
            Text(
                text = mood,
                color = if (isSelected) HomeColors.OnAccent else HomeColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) HomeColors.Accent else HomeColors.CardHigh)
                    .clickable { onSelect(if (isSelected) null else mood) }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            )
        }
    }
}

/* ---------------------------------------------------------------
 *  HERO (featured track)
 * --------------------------------------------------------------- */

@Composable
private fun HeroCard(song: HomeSong, onPlay: (HomeSong) -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(HomeColors.Card)
            .clickable { onPlay(song) }
    ) {
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.25f to Color(0x22000000),
                    1f to Color(0xF2060410)
                )
            )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.75f)
                .padding(start = 22.dp, bottom = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = HomeColors.Accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "FEATURED TRACK",
                    color = HomeColors.Accent,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                song.title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                listOf(song.artist, song.durationText).filter { it.isNotBlank() }.joinToString("  •  "),
                color = Color(0xFFD9D2E8),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(HomeColors.Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, "Play", tint = HomeColors.OnAccent, modifier = Modifier.size(32.dp))
        }
    }
}

/* ---------------------------------------------------------------
 *  QUICK ACCESS (0 wale stats card ki jagah)
 * --------------------------------------------------------------- */

private class AccessItem(
    val icon: ImageVector,
    val label: String,
    val count: Int,
    val onClick: () -> Unit
)

@Composable
private fun QuickAccessRow(state: HomeUiState, actions: HomeActions) {
    val items = listOf(
        AccessItem(Icons.Rounded.Favorite, "Liked", state.likedCount, actions.onOpenLiked),
        AccessItem(Icons.Rounded.History, "Recent", state.recent.size, actions.onOpenRecent),
        AccessItem(Icons.Rounded.Download, "Downloads", state.downloadCount, actions.onOpenDownloads),
        AccessItem(Icons.Rounded.QueueMusic, "Playlists", state.playlistCount, actions.onOpenPlaylists)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { item ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(HomeColors.Card)
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, null, tint = HomeColors.Accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(item.label, color = HomeColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (item.count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.count.toString(),
                        color = HomeColors.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------------
 *  SECTIONS + CARDS
 * --------------------------------------------------------------- */

@Composable
private fun Section(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = HomeColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, color = HomeColors.TextSecondary, fontSize = 13.sp)
                }
            }
            if (actionLabel != null) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HomeColors.CardHigh)
                        .clickable(onClick = onAction)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shuffle, null, tint = HomeColors.Accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(actionLabel, color = HomeColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun SongRow(songs: List<HomeSong>, cardSize: Dp, actions: HomeActions) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(songs, key = { it.id }) { song -> SongCard(song, cardSize, actions) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongCard(song: HomeSong, size: Dp, actions: HomeActions) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(),
        label = "press"
    )
    var menuOpen by remember { mutableStateOf(false) }

    Column(Modifier.width(size).scale(scale)) {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(20.dp))
                .background(HomeColors.Card)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { actions.onPlaySong(song) },
                    onLongClick ={ menuOpen = true }
                )
        ) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Play next") },
                    leadingIcon = { Icon(Icons.Rounded.SkipNext, null) },
                    onClick = { menuOpen = false; actions.onPlayNext(song) }
                )
                DropdownMenuItem(
                    text = { Text("Add to queue") },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                    onClick = { menuOpen = false; actions.onAddToQueue(song) }
                )
                DropdownMenuItem(
                    text = { Text(if (song.isLiked) "Unlike" else "Like") },
                    leadingIcon = {
                        Icon(if (song.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null)
                    },
                    onClick = { menuOpen = false; actions.onToggleLike(song) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            song.title,
            color = HomeColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            song.artist.ifBlank { "Song" },
            color = HomeColors.TextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistCard(playlist: HomePlaylist, onClick: () -> Unit) {
    val urls = playlist.artworkUrls.take(4)
    Column(Modifier.width(170.dp)) {
        Box(
            Modifier
                .size(170.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(HomeColors.Card)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                urls.size >= 4 -> Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Art(urls[0], Modifier.weight(1f).fillMaxHeight())
                        Art(urls[1], Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Art(urls[2], Modifier.weight(1f).fillMaxHeight())
                        Art(urls[3], Modifier.weight(1f).fillMaxHeight())
                    }
                }
                urls.isNotEmpty() -> Art(urls[0], Modifier.fillMaxSize())
                else -> Icon(Icons.Rounded.QueueMusic, null, tint = HomeColors.Accent, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            playlist.title,
            color = HomeColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (playlist.subtitle.isNotBlank()) {
            Text(
                playlist.subtitle,
                color = HomeColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Art(url: String, modifier: Modifier) {
    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

/* ---------------------------------------------------------------
 *  LOADING / EMPTY / FOOTER
 * --------------------------------------------------------------- */

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing)),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(Color(0xFF1B1726), Color(0xFF2B2439), Color(0xFF1B1726)),
        start = Offset(x - 400f, 0f),
        end = Offset(x, 0f)
    )
}

@Composable
private fun HomeSkeleton() {
    val brush = shimmerBrush()
    Column(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(230.dp)
                .clip(RoundedCornerShape(30.dp)).background(brush)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) {
                Box(
                    Modifier.weight(1f).aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp)).background(brush)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Abhi kuch nahi mila",
            color = HomeColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Internet check karke refresh karo.",
            color = HomeColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeColors.Accent,
                contentColor = HomeColors.OnAccent
            )
        ) { Text("Refresh") }
    }
}

@Composable
private fun HomeFooter() {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "MADE FOR YOUR MOMENTS",
            color = HomeColors.TextSecondary.copy(alpha = 0.6f),
            fontSize = 11.sp,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "MUSIC NOVA",
            color = HomeColors.Accent.copy(alpha = 0.8f),
            fontSize = 14.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
