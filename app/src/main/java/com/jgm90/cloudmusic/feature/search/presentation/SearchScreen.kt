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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
    val historyScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(SearchMode.Home) }
    var addToPlaylistSong by remember { mutableStateOf<SongModel?>(null) }
    var history by remember { mutableStateOf(emptyList<String>()) }
    var showHistory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        history = historyStore.getHistory()
    }

    fun submitSearch() {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        historyScope.launch {
            historyStore.addQuery(trimmed)
            history = historyStore.getHistory()
        }
        viewModel.search(trimmed)
        mode = SearchMode.Results
    }

    if (mode == SearchMode.Results) {
        LaunchedEffect(mode) {
            onSearchActiveChange(true)
        }
        SearchResultsScreen(
            query = query,
            onQueryChange = { query = it },
            onClearQuery = { query = "" },
            state = state,
            onBack = {
                mode = SearchMode.Home
                onSearchActiveChange(false)
            },
            onOpenNowPlaying = onOpenNowPlaying,
            onSearch = { submitSearch() },
            onLoadMore = { viewModel.loadMore() },
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
            onSearchActiveChange(false)
        }
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SearchHomeTopBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { submitSearch() },
                    onFocusChange = { focused -> showHistory = focused },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (showHistory) {
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                TextButton(onClick = {
                                    historyScope.launch {
                                        historyStore.clear()
                                        history = emptyList()
                                    }
                                }) {
                                    Text(
                                        text = stringResource(id = R.string.search_history_clear),
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(text = item, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
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
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    state: SearchViewState,
    onBack: () -> Unit,
    onOpenNowPlaying: (Int, List<SongModel>) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onDownload: (SongModel) -> Unit,
    onAddToPlaylist: (SongModel) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.searchResults.size, state.hasMore, state.isLoadingMore) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                val totalCount = listState.layoutInfo.totalItemsCount
                if (state.hasMore && !state.isLoadingMore && totalCount > 0) {
                    if (lastVisibleIndex >= totalCount - 5) {
                        onLoadMore()
                    }
                }
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                            focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
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
                LazyColumn(
                    state = listState,
                    contentPadding = padding,
                ) {
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
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHomeTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) },
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.search_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                        )
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
