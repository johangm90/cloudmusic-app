package com.jgm90.cloudmusic.feature.playback.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.core.event.PlaybackInfoEvent
import kotlinx.coroutines.Job

@Composable
fun PlaybackControlsBar(
    onOpenNowPlaying: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val title = remember { mutableStateOf("") }
    val subtitle = remember { mutableStateOf("") }
    val artUrl = remember { mutableStateOf("") }
    val isPlaying = remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val jobs = listOf(
            AppEventBus.observe<PlaybackInfoEvent>(scope) { event ->
                title.value = event.title
                subtitle.value = event.artist
                artUrl.value = event.artUrl
                isPlaying.value = event.isPlaying
            },
        )

        onDispose {
            jobs.forEach(Job::cancel)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(12.dp)
            .clickable { onOpenNowPlaying() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = artUrl.value,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title.value,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle.value,
                color = MaterialTheme.colorScheme.onPrimary.copy(
                    alpha = 0.6f,
                ),
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Icon(
            painter = painterResource(
                if (isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play_arrow
            ),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .clickable {
                    AppEventBus.post(PlayPauseEvent("From Playback Controls"))
                },
        )
    }
}
