package com.jgm90.cloudmusic.feature.playlist.presentation

import android.app.DownloadManager
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.DownloadEvent
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.feature.playlist.presentation.dialogs.AddToPlaylistDialog
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.LibraryViewModel
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel
import io.nubit.cloudmusic.designsystem.component.EmptyState
import io.nubit.cloudmusic.designsystem.component.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyPlayedScreen(
    onBack: () -> Unit,
    onOpenNowPlaying: (Int, List<SongModel>) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    var songs by remember { mutableStateOf<List<SongModel>>(emptyList()) }
    var addToPlaylistSong by remember { mutableStateOf<SongModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadRecent { songs = it }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.recently_played)) },
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
        if (songs.isEmpty()) {
            EmptyState(
                textRes = R.string.no_recent_plays,
                imageRes = R.drawable.ic_info_black_24dp,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(contentPadding = padding) {
                itemsIndexed(songs) { index, song ->
                    SongItem(
                        imageUrl = song.getCoverThumbnail(),
                        songName = song.name,
                        artistName = song.artist,
                        albumName = song.album,
                        onClick = { onOpenNowPlaying(index, songs) },
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
                        onAddToPlaylistClick = { addToPlaylistSong = song },
                    )
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
fun LikedSongsScreen(
    onBack: () -> Unit,
    onOpenNowPlaying: (Int, List<SongModel>) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    var songs by remember { mutableStateOf<List<SongModel>>(emptyList()) }
    var addToPlaylistSong by remember { mutableStateOf<SongModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLiked { songs = it }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.liked_songs)) },
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
        if (songs.isEmpty()) {
            EmptyState(
                textRes = R.string.no_liked_songs,
                imageRes = R.drawable.ic_info_black_24dp,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(contentPadding = padding) {
                itemsIndexed(songs) { index, song ->
                    SongItem(
                        imageUrl = song.getCoverThumbnail(),
                        songName = song.name,
                        artistName = song.artist,
                        albumName = song.album,
                        onClick = { onOpenNowPlaying(index, songs) },
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
                        onAddToPlaylistClick = { addToPlaylistSong = song },
                    )
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
