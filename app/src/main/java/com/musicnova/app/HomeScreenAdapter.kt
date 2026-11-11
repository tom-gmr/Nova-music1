
package com.musicnova.app

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.ui.home.HomeActions
import com.musicnova.app.ui.home.HomePlaylist
import com.musicnova.app.ui.home.HomeScreen
import com.musicnova.app.ui.home.HomeSong
import com.musicnova.app.ui.home.HomeUiState
import com.musicnova.app.ui.home.MusicViewModel

@Composable
fun RecommendationsHomeScreen(
    onPlay: (YouTubeMusicTrack) -> Unit,
    onBrowse: (String) -> Unit,
    onPlayNext: (YouTubeMusicTrack) -> Unit = {},
    onAddToQueue: (YouTubeMusicTrack) -> Unit = {}
) {
    val viewModel: MusicViewModel = hiltViewModel()
    val musicState by viewModel.uiState.collectAsState()

    var selectedMood by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadHomeMusic()
    }

    fun YouTubeMusicTrack.toHomeSong() = HomeSong(
        id = videoId,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl
    )

    val nativeTracks = musicState.nativeHomePage
        ?.shelves
        ?.flatMap { shelf ->
            shelf.items.mapNotNull { item ->
                (item as? com.musicnova.app.data.music.HomeShelfItem.Track)
                    ?.track
            }
        }
        .orEmpty()

    val playlists = musicState.nativeHomePage
        ?.shelves
        ?.flatMap { shelf ->
            shelf.items.mapNotNull { item ->
                val playlist =
                    item as? com.musicnova.app.data.music.HomeShelfItem.Playlist

                playlist?.let {
                    HomePlaylist(
                        id = it.playlist.id,
                        title = it.playlist.title,
                        subtitle = it.playlist.author.orEmpty(),
                        artworkUrls = it.collageArtworkUrls.ifEmpty {
                            listOfNotNull(it.playlist.artworkUrl)
                        }
                    )
                }
            }
        }
        .orEmpty()
        .distinctBy { it.id }

    val homeState = HomeUiState(
        isLoading = musicState.isLoading,
        featured = musicState.recommendations.firstOrNull()?.toHomeSong(),
        recent = musicState.recentlyPlayed.map {
            HomeSong(
                id = it.videoId,
                title = it.title,
                artist = it.artist,
                artworkUrl = it.artworkUrl
            )
        },
        quickPicks = nativeTracks.map { it.toHomeSong() },
        madeForYou = musicState.recommendations.map { it.toHomeSong() },
        trending = musicState.trending.map { it.toHomeSong() },
        communityPlaylists = playlists,
        likedCount = musicState.likedCount,
        playlistCount = playlists.size
    )

    val actions = HomeActions(
        onPlaySong = { song ->
            val track = (musicState.recommendations +
                musicState.trending + nativeTracks)
                .firstOrNull { it.videoId == song.id }

            track?.let(onPlay)
        },
        onPlayNext = { song ->
            val track = (musicState.recommendations +
                musicState.trending + nativeTracks)
                .firstOrNull { it.videoId == song.id }

            track?.let(onPlayNext)
        },
        onAddToQueue = { song ->
            val track = (musicState.recommendations +
                musicState.trending + nativeTracks)
                .firstOrNull { it.videoId == song.id }

            track?.let(onAddToQueue)
        },
        onToggleLike = { song ->
            val track = (musicState.recommendations +
                musicState.trending + nativeTracks)
                .firstOrNull { it.videoId == song.id }

            track?.let(viewModel::toggleLike)
        },
        onPlaylistClick = { playlist ->
            onBrowse(playlist.id)
        },
        onRefresh = viewModel::refresh,
        onMoodSelected = { mood ->
            selectedMood = mood
        }
    )

    HomeScreen(
        state = homeState,
        actions = actions,
        modifier = Modifier,
        selectedMood = selectedMood
    )
}
