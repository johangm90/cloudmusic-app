package com.jgm90.cloudmusic.feature.discover.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.feature.discover.presentation.viewmodel.DiscoverViewModel
import io.nubit.cloudmusic.designsystem.component.EmptyState
import io.nubit.cloudmusic.designsystem.component.Loader
import io.nubit.cloudmusic.designsystem.component.SongItem

@Composable
fun DiscoverScreen(
    onOpenNowPlaying: (Int, List<SongModel>) -> Unit,
    onOpenArtist: (String, String?) -> Unit,
    onOpenAlbum: (String, String?, String?) -> Unit,
    onPlayNext: (SongModel) -> Unit,
    onAddToQueue: (SongModel) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Loader(state.isLoading) {
        if (state.error != null && state.homeSongs.isEmpty() && state.chartSongs.isEmpty()) {
            EmptyState(
                textRes = R.string.discover_empty,
                imageRes = R.drawable.ic_info_black_24dp,
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                item {
                    SectionTitle(text = stringResource(R.string.discover_home))
                }
                itemsIndexed(state.homeSongs) { index, song ->
                    SongItem(
                        imageUrl = song.getCoverThumbnail(),
                        songName = song.name,
                        artistName = song.artist,
                        albumName = song.album,
                        onClick = { onOpenNowPlaying(index, state.homeSongs) },
                        onArtistClick = { artistName -> onOpenArtist(artistName, song.artist_id) },
                        onAlbumClick = { albumName -> onOpenAlbum(albumName, song.album_id, song.artist.firstOrNull()) },
                        onPlayNextClick = { onPlayNext(song) },
                        onAddToQueueClick = { onAddToQueue(song) },
                    )
                }

                item {
                    SectionTitle(text = stringResource(R.string.discover_charts))
                }
                itemsIndexed(state.chartSongs) { index, song ->
                    SongItem(
                        imageUrl = song.getCoverThumbnail(),
                        songName = song.name,
                        artistName = song.artist,
                        albumName = song.album,
                        onClick = { onOpenNowPlaying(index, state.chartSongs) },
                        onArtistClick = { artistName -> onOpenArtist(artistName, song.artist_id) },
                        onAlbumClick = { albumName -> onOpenAlbum(albumName, song.album_id, song.artist.firstOrNull()) },
                        onPlayNextClick = { onPlayNext(song) },
                        onAddToQueueClick = { onAddToQueue(song) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
