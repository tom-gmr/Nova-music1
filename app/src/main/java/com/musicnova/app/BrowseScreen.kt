package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicnova.app.data.music.HomePage
import com.musicnova.app.data.music.MusicRepository
import com.musicnova.app.data.music.YouTubeMusicTrack
import com.musicnova.app.ui.feed.HomeNativeShelves
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val BrowseBackground = Color(0xFF09080E)
private val BrowseSurface = Color(0xFF191520)
private val BrowseText = Color(0xFFF8F5FC)
private val BrowseMuted = Color(0xFFAAA4B5)
private val BrowseLavender = Color(0xFFD1C1EB)
private val BrowseBorder = Color(0xFF393044)

data class BrowseUiState(
    val isLoading: Boolean = false,
    val page: HomePage? = null,
    val error: String? = null
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())

    val uiState: StateFlow<BrowseUiState> =
        _uiState.asStateFlow()

    private var browseJob: Job? = null

    fun loadBrowsePage(browseId: String) {
        if (browseId.isBlank()) {
            _uiState.value = BrowseUiState(
                error = "Invalid Browse ID"
            )
            return
        }

        browseJob?.cancel()

        browseJob = viewModelScope.launch {
            _uiState.value = BrowseUiState(
                isLoading = true
            )

            try {
                val page = repository.fetchBrowsePage(browseId)

                _uiState.value = BrowseUiState(
                    isLoading = false,
                    page = page
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = BrowseUiState(
                    isLoading = false,
                    error = e.message ?: "Unable to load this page"
                )
            }
        }
    }
}

@Composable
fun BrowseScreen(
    browseId: String,
    onBack: () -> Unit,
    onPlay: (YouTubeMusicTrack) -> Unit,
    onBrowse: (String) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(browseId) {
        viewModel.loadBrowsePage(browseId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrowseBackground)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 13.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrowseSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = BrowseLavender
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "MUSIC NOVA",
                    color = BrowseMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "Explore",
                    color = BrowseText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            IconButton(
                onClick = {
                    viewModel.loadBrowsePage(browseId)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrowseSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh page",
                    tint = BrowseLavender
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 5.dp
                )
                .clip(RoundedCornerShape(19.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF30253D),
                            Color(0xFF191520)
                        )
                    )
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Color.White.copy(alpha = 0.09f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = BrowseLavender,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = "DISCOVER SOMETHING NEW",
                    color = BrowseLavender,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Your next favourite sound.",
                    color = BrowseText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        when {

            uiState.isLoading -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(BrowseSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = BrowseLavender,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(29.dp)
                            )
                        }

                        Spacer(Modifier.height(17.dp))

                        Text(
                            text = "Discovering music...",
                            color = BrowseText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = "Finding something special for you",
                            color = BrowseMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            uiState.error != null -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(25.dp))
                            .background(BrowseSurface)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = BrowseLavender,
                            modifier = Modifier.size(43.dp)
                        )

                        Spacer(Modifier.height(17.dp))

                        Text(
                            text = "Couldn't load this page",
                            color = BrowseText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = uiState.error.orEmpty(),
                            color = BrowseMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(19.dp))

                        IconButton(
                            onClick = {
                                viewModel.loadBrowsePage(browseId)
                            },
                            modifier = Modifier
                                .size(49.dp)
                                .clip(CircleShape)
                                .background(
                                    Color(0xFF393044)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Try again",
                                tint = BrowseLavender,
                                modifier = Modifier.size(25.dp)
                            )
                        }

                        Text(
                            text = "Tap to try again",
                            color = BrowseMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            uiState.page != null -> {

                val shelves = uiState.page?.shelves.orEmpty()

                if (shelves.isEmpty()) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BrowseLavender,
                                modifier = Modifier.size(38.dp)
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "No music found",
                                color = BrowseText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = "Try exploring another collection.",
                                color = BrowseMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                } else {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {

                        HomeNativeShelves(
                            shelves = shelves,
                            onPlay = onPlay,
                            onBrowse = onBrowse,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(
                            modifier = Modifier.height(35.dp)
                        )
                    }
                }
            }

            else -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Choose a music collection to explore.",
                        color = BrowseMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
