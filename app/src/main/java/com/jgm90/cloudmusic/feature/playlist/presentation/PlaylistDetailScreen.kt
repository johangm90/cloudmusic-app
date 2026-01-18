package com.jgm90.cloudmusic.feature.playlist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.ui.theme.AppBackground
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel
import io.nubit.cloudmusic.designsystem.component.EmptyState
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    playlistName: String,
    playlistOffline: Int,
    showOfflineToggle: Boolean,
    onBack: () -> Unit,
    onPlaySong: (Int, List<SongModel>) -> Unit,
    onDownloadSong: (SongModel) -> Unit,
    onDeleteSong: (SongModel) -> Unit,
    onToggleOffline: (Boolean) -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    var songs by remember { mutableStateOf<List<SongModel>>(emptyList()) }
    var offline by remember { mutableStateOf(playlistOffline == 1) }
    var pendingDelete by remember { mutableStateOf<SongModel?>(null) }

    LaunchedEffect(playlistId) {
        viewModel.loadSongs(playlistId) { songs = it }
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(text = playlistName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showOfflineToggle) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = "Disponible offline", color = Color.White)
                            Switch(
                                checked = offline,
                                onCheckedChange = {
                                    offline = it
                                    onToggleOffline(it)
                                },
                            )
                        }
                    }
                }

                if (songs.isEmpty()) {
                    EmptyState(
                        textRes = R.string.no_songs,
                        imageRes = R.drawable.ic_info_black_24dp,
                    )
                } else {
                    LazyColumn {
                        itemsIndexed(songs) { index, song ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { onPlaySong(index, songs) },
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White
                                        )
                                        Text(
                                            text = song.artist.joinToString(","),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f),
                                        )
                                    }
                                    var menuExpanded by remember(song.id) { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_more_vert_black_24dp),
                                                contentDescription = null,
                                                tint = Color.White,
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                            offset = DpOffset(x = 0.dp, y = 4.dp),
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(text = stringResource(id = R.string.delete)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    pendingDelete = song
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        val current = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = stringResource(id = R.string.delete_song_title)) },
            text = { Text(text = stringResource(id = R.string.delete_song_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSong(current)
                    songs = songs.filterNot { it.id == current.id }
                    pendingDelete = null
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
