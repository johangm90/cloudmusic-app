package com.jgm90.cloudmusic.feature.playlist.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import com.jgm90.cloudmusic.feature.playlist.presentation.dialogs.PlaylistDialog
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel
import io.nubit.cloudmusic.designsystem.component.EmptyState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History

@Composable
fun LibraryScreen(
    onOpenRecent: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenPlaylist: (PlaylistModel) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    var playlists by remember { mutableStateOf<List<PlaylistModel>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editPlaylist by remember { mutableStateOf<PlaylistModel?>(null) }
    var pendingDelete by remember { mutableStateOf<PlaylistModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPlaylists { playlists = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(id = R.string.recently_played)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Filled.History, contentDescription = null)
                    },
                    modifier = Modifier
                        .clickable { onOpenRecent() }
                        .padding(horizontal = 8.dp),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(id = R.string.liked_songs)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = null)
                    },
                    modifier = Modifier
                        .clickable { onOpenLiked() }
                        .padding(horizontal = 8.dp),
                )
            }

            if (playlists.isEmpty()) {
                item {
                    EmptyState(
                        textRes = R.string.no_playlists,
                        imageRes = R.drawable.ic_info_black_24dp,
                    )
                }
            } else {
                itemsIndexed(playlists) { _, playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onOpenPlaylist = { onOpenPlaylist(playlist) },
                        onEdit = { editPlaylist = playlist },
                        onDelete = { pendingDelete = playlist },
                    )
                }
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
            title = stringResource(id = R.string.playlists),
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
            title = stringResource(id = R.string.edit),
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

    if (pendingDelete != null) {
        val current = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = stringResource(id = R.string.delete_playlist_title)) },
            text = { Text(text = stringResource(id = R.string.delete_playlist_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlaylist(current) {
                        viewModel.loadPlaylists { playlists = it }
                        pendingDelete = null
                    }
                }) { Text(text = stringResource(id = R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistModel,
    onOpenPlaylist: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(text = playlist.name) },
        supportingContent = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.playlist_messages,
                    count = playlist.song_count,
                    playlist.song_count,
                )
            )
        },
        leadingContent = {
            if (playlist.offline == 1) {
                Text(
                    text = "Offline",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_black_24dp),
                        contentDescription = null,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.edit)) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.delete)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        },
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onOpenPlaylist() },
    )
}
