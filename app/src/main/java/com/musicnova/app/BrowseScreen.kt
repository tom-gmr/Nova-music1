package com.musicnova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
private val BrowseText = Color(0xFFF7F4FA)
private val BrowseMuted = Color(0xFFAAA4B5)
private val BrowseLavender = Color(0xFFD1C1EB)

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
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = BrowseLavender
                )
            }

            Text(
                text = "EXPLORE MUSIC",
                color = BrowseText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    viewModel.loadBrowsePage(browseId)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh page",
                    tint = BrowseLavender
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
                        CircularProgressIndicator(
                            color = BrowseLavender,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )

                        Text(
                            text = "Loading music...",
                            color = BrowseMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 14.dp)
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Couldn't load this page",
                            color = BrowseText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = uiState.error.orEmpty(),
                            color = BrowseMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                top = 9.dp,
                                bottom = 18.dp
                            )
                        )

                        IconButton(
                            onClick = {
                                viewModel.loadBrowsePage(browseId)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Try again",
                                tint = BrowseLavender,
                                modifier = Modifier.size(30.dp)
                            )
                        }
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
                        Text(
                            text = "No music found on this page.",
                            color = BrowseMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        HomeNativeShelves(
                            shelves = shelves,
                            onPlay = onPlay,
                            onBrowse = onBrowse,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(
                            modifier = Modifier.padding(bottom = 28.dp)
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
