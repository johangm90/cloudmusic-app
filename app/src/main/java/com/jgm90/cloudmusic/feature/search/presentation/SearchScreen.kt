package com.jgm90.cloudmusic.feature.search.presentation

import android.app.DownloadManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.DownloadEvent
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.ui.theme.CloudMusicTheme
import com.jgm90.cloudmusic.feature.playlist.presentation.dialogs.AddToPlaylistDialog
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel
import com.jgm90.cloudmusic.feature.search.presentation.viewmodel.SearchViewModel
import io.nubit.cloudmusic.designsystem.component.EmptyState
import io.nubit.cloudmusic.designsystem.component.Loader
import io.nubit.cloudmusic.designsystem.component.SongItem

@Composable
fun SearchScreen(
    onOpenNowPlaying: (Int, List<SongModel>) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    var addToPlaylistSong by remember { mutableStateOf<SongModel?>(null) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = { Text(text = "Buscar") },
                singleLine = true,
            )
            Button(onClick = { viewModel.search(query) }) {
                Text(text = "Buscar")
            }
        }

        CloudMusicTheme {
            Loader(state.isLoading) {
                if (state.error != null) {
                    EmptyState(
                        textRes = R.string.error_retrofit,
                        imageRes = R.drawable.ic_error_black_24dp,
                    )
                } else if (state.searchResults.isEmpty()) {
                    EmptyState(
                        textRes = R.string.search_message,
                        imageRes = R.drawable.ic_search_black_24dp,
                    )
                }

                LazyColumn {
                    itemsIndexed(state.searchResults) { index, song ->
                        SongItem(
                            imageUrl = song.getCoverThumbnail(),
                            songName = song.name,
                            artistName = song.artist,
                            albumName = song.album,
                            onClick = {
                                onOpenNowPlaying(index, state.searchResults)
                            },
                            onDownloadClick = {
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
                            onAddToPlaylistClick = {
                                addToPlaylistSong = song
                            },
                        )
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
