package com.jgm90.cloudmusic.feature.artist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.feature.artist.presentation.viewmodel.ArtistViewModel
import io.nubit.cloudmusic.designsystem.component.EmptyState
import io.nubit.cloudmusic.designsystem.component.Loader
import io.nubit.cloudmusic.designsystem.component.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistName: String,
    artistId: String?,
    onBack: () -> Unit,
    onOpenNowPlaying: (Int, List<SongModel>) -> Unit,
    onOpenArtist: (String, String?) -> Unit,
    onOpenAlbum: (String, String?, String?) -> Unit,
    onPlayNext: (SongModel) -> Unit,
    onAddToQueue: (SongModel) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(artistName, artistId) {
        viewModel.loadArtist(artistName, artistId)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(text = state.artistName.ifBlank { artistName }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                            contentDescription = null,
                        )
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
            if (state.error != null && state.songs.isEmpty()) {
                EmptyState(
                    textRes = R.string.artist_load_failed,
                    imageRes = R.drawable.ic_error_black_24dp,
                    modifier = Modifier.padding(padding),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        end = 0.dp,
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                ) {
                    item {
                        ArtistHeader(
                            name = state.artistName.ifBlank { artistName },
                            subtitle = stringResource(R.string.artist_top_songs),
                            imageUrl = state.artistImageUrl,
                        )
                    }

                    if (state.songs.isEmpty()) {
                        item {
                            EmptyState(
                                textRes = R.string.no_artist_songs,
                                imageRes = R.drawable.ic_info_black_24dp,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    } else {
                        itemsIndexed(state.songs) { index, song ->
                            SongItem(
                                imageUrl = song.getCoverThumbnail(),
                                songName = song.name,
                                artistName = song.artist,
                                albumName = song.album,
                                onClick = { onOpenNowPlaying(index, state.songs) },
                                onArtistClick = { selectedName ->
                                    onOpenArtist(selectedName, song.artist_id)
                                },
                                onAlbumClick = { album ->
                                    onOpenAlbum(album, song.album_id, song.artist.firstOrNull())
                                },
                                onPlayNextClick = { onPlayNext(song) },
                                onAddToQueueClick = { onAddToQueue(song) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    name: String,
    subtitle: String,
    imageUrl: String,
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsyncImage(
                model = imageUrl,
                placeholder = painterResource(io.nubit.cloudmusic.designsystem.R.drawable.default_cover),
                fallback = painterResource(io.nubit.cloudmusic.designsystem.R.drawable.default_cover),
                error = painterResource(io.nubit.cloudmusic.designsystem.R.drawable.default_cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
