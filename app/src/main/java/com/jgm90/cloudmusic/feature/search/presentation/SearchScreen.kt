package com.jgm90.cloudmusic.feature.search.presentation

import android.app.DownloadManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.DownloadEvent
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.feature.playlist.presentation.dialogs.AddToPlaylistDialog
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel
import com.jgm90.cloudmusic.feature.search.data.SearchHistoryStore
import com.jgm90.cloudmusic.feature.search.presentation.viewmodel.SearchViewModel
import com.jgm90.cloudmusic.feature.search.presentation.viewmodel.SearchViewState
import io.nubit.cloudmusic.designsystem.component.EmptyState
import io.nubit.cloudmusic.designsystem.component.Loader
import io.nubit.cloudmusic.designsystem.component.SongItem

private enum class SearchMode {
    Home,
    Results,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenNowPlaying: (Int, List<SongModel>) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val historyStore = remember { SearchHistoryStore(context) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(SearchMode.Home) }
    var addToPlaylistSong by remember { mutableStateOf<SongModel?>(null) }
    var history by remember { mutableStateOf(historyStore.getHistory()) }

    fun setActive(expanded: Boolean) {
        active = expanded
        onSearchActiveChange(expanded || mode == SearchMode.Results)
    }

    fun submitSearch() {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        historyStore.addQuery(trimmed)
        history = historyStore.getHistory()
        viewModel.search(trimmed)
        setActive(false)
        mode = SearchMode.Results
    }

    if (mode == SearchMode.Results) {
        LaunchedEffect(mode) {
            onSearchActiveChange(true)
        }
        SearchResultsScreen(
            query = query,
            state = state,
            onBack = {
                mode = SearchMode.Home
                onSearchActiveChange(false)
            },
            onOpenNowPlaying = onOpenNowPlaying,
            onDownload = { song ->
                AppEventBus.postSticky(
                    DownloadEvent(
                        true,
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                        song.getAudioUrl(),
                        song.name,
                        song.getFileName(),
                    )
                )
            },
            onAddToPlaylist = { addToPlaylistSong = it },
        )
    } else {
        LaunchedEffect(mode) {
            onSearchActiveChange(active)
        }
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CompositionLocalProvider(
                    LocalContentColor provides Color.White,
                    LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                ) {
                    SearchBar(
                        modifier = Modifier.fillMaxWidth(),
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = query,
                                onQueryChange = { query = it },
                                onSearch = { submitSearch() },
                                expanded = active,
                                onExpandedChange = { setActive(it) },
                                placeholder = {
                                    Text(
                                        text = stringResource(id = R.string.search_hint),
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                },
                            )
                        },
                        expanded = active,
                        onExpandedChange = { setActive(it) },
                        colors = SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                            dividerColor = Color.Transparent,
                        ),
                    ) {
                    if (history.isEmpty()) {
                        EmptyState(
                            textRes = R.string.search_history_empty,
                            imageRes = R.drawable.ic_search_black_24dp,
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                            Text(
                                text = stringResource(id = R.string.search_history_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                            )
                                TextButton(onClick = {
                                    historyStore.clear()
                                    history = emptyList()
                                }) {
                                Text(
                                    text = stringResource(id = R.string.search_history_clear),
                                    color = Color.White,
                                )
                            }
                        }
                            history.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            query = item
                                            submitSearch()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White.copy(alpha = 0.8f),
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(text = item, color = Color.White)
                                }
                            }
                        }
                    }
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding))
        }
    }

    if (addToPlaylistSong != null) {
        AddToPlaylistDialog(
            song = addToPlaylistSong!!,
            viewModel = playlistViewModel,
            onDismiss = { addToPlaylistSong = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultsScreen(
    query: String,
    state: SearchViewState,
    onBack: () -> Unit,
    onOpenNowPlaying: (Int, List<SongModel>) -> Unit,
    onDownload: (SongModel) -> Unit,
    onAddToPlaylist: (SongModel) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(text = query) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Loader(state.isLoading) {
            if (state.error != null) {
                EmptyState(
                    textRes = R.string.error_retrofit,
                    imageRes = R.drawable.ic_error_black_24dp,
                    modifier = Modifier.padding(padding),
                )
            } else if (state.searchResults.isEmpty()) {
                EmptyState(
                    textRes = R.string.search_message,
                    imageRes = R.drawable.ic_search_black_24dp,
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(contentPadding = padding) {
                    itemsIndexed(state.searchResults) { index, song ->
                        SongItem(
                            imageUrl = song.getCoverThumbnail(),
                            songName = song.name,
                            artistName = song.artist,
                            albumName = song.album,
                            onClick = {
                                onOpenNowPlaying(index, state.searchResults)
                            },
                            onDownloadClick = { onDownload(song) },
                            onAddToPlaylistClick = { onAddToPlaylist(song) },
                        )
                    }
                }
            }
        }
    }
}
