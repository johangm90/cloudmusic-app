package com.jgm90.cloudmusic.feature.playlist.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import com.jgm90.cloudmusic.feature.playlist.presentation.dialogs.PlaylistDialog
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel

@Composable
fun PlaylistScreen(
    onOpenPlaylist: (PlaylistModel) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    var playlists by remember { mutableStateOf<List<PlaylistModel>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editPlaylist by remember { mutableStateOf<PlaylistModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPlaylists { playlists = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn {
            items(playlists) { playlist ->
                ListItem(
                    headlineContent = { Text(text = playlist.name) },
                    supportingContent = { Text(text = "${playlist.song_count} canciones") },
                    trailingContent = {
                        androidx.compose.foundation.layout.Row {
                            if (playlist.offline == 1) {
                                Text(
                                    text = "Offline",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                            IconButton(onClick = { editPlaylist = playlist }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert_black_24dp),
                                    contentDescription = null,
                                )
                            }
                            IconButton(onClick = {
                                viewModel.deletePlaylist(playlist) {
                                    viewModel.loadPlaylists { playlists = it }
                                }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_error_black_24dp),
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                        .clickable { onOpenPlaylist(playlist) },
                )
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(painter = painterResource(R.drawable.ic_add_24dp), contentDescription = null)
        }
    }

    if (showCreateDialog) {
        PlaylistDialog(
            title = "Playlist",
            initialName = "",
            onConfirm = { name ->
                viewModel.savePlaylist(
                    PlaylistModel(0, name, 0, 0)
                ) {
                    viewModel.loadPlaylists { playlists = it }
                    showCreateDialog = false
                }
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    if (editPlaylist != null) {
        val current = editPlaylist!!
        PlaylistDialog(
            title = "Editar playlist",
            initialName = current.name,
            onConfirm = { name ->
                current.name = name
                viewModel.savePlaylist(current) {
                    viewModel.loadPlaylists { playlists = it }
                    editPlaylist = null
                }
            },
            onDismiss = { editPlaylist = null },
        )
    }
}
