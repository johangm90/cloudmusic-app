package com.jgm90.cloudmusic.feature.queue.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.feature.queue.presentation.viewmodel.QueueViewModel
import io.nubit.cloudmusic.designsystem.component.EmptyState
import io.nubit.cloudmusic.designsystem.component.SongItem

@Composable
fun QueueScreen(
    onOpenNowPlaying: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val state = viewModel.queueState.collectAsStateWithLifecycle().value

    if (state.queue.isEmpty()) {
        EmptyState(
            textRes = R.string.queue_empty,
            imageRes = R.drawable.ic_info_black_24dp,
        )
        return
    }

    LazyColumn {
        itemsIndexed(state.queue) { index, song ->
            SongItem(
                imageUrl = song.getCoverThumbnail(),
                songName = song.name,
                artistName = song.artist,
                albumName = song.album,
                isPlaying = index == state.index,
                onClick = {
                    viewModel.select(index)
                    onOpenNowPlaying()
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (index == state.index) {
                    Text(
                        text = stringResource(R.string.queue_now_playing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(text = "")
                }

                Row {
                    TextButton(onClick = { viewModel.moveUp(index) }) {
                        Text(stringResource(R.string.move_up))
                    }
                    TextButton(onClick = { viewModel.moveDown(index) }) {
                        Text(stringResource(R.string.move_down))
                    }
                    TextButton(onClick = { viewModel.remove(index) }) {
                        Text(stringResource(R.string.remove_from_queue))
                    }
                }
            }
        }
    }
}
