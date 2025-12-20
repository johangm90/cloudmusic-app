package com.jgm90.cloudmusic.feature.playlist.presentation.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel

@Composable
fun AddToPlaylistDialog(
    song: SongModel,
    viewModel: PlaylistViewModel,
    onDismiss: () -> Unit,
) {
    var playlists by remember { mutableStateOf<List<PlaylistModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.loadPlaylists { playlists = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add to playlist") },
        text = {
            LazyColumn {
                items(playlists) { playlist ->
                    TextButton(
                        onClick = {
                            viewModel.addSongToPlaylist(song, playlist.playlist_id) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Text(text = playlist.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cerrar")
            }
        },
    )
}
