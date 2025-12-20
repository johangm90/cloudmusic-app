package com.jgm90.cloudmusic.feature.playback.presentation

import android.app.Activity
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import kotlinx.coroutines.Job

@Composable
fun PlaybackControlsBar(
    onOpenNowPlaying: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val title = remember { mutableStateOf(MediaPlayerService.getSongName()) }
    val subtitle = remember { mutableStateOf(MediaPlayerService.getSongArtists()) }
    val artUrl = remember { mutableStateOf(MediaPlayerService.getAlbumArtUrl()) }
    val isPlaying = remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val controller = MediaControllerCompat.getMediaController(activity!!)
        val callback = object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                isPlaying.value = state.state == PlaybackStateCompat.STATE_PLAYING
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                if (metadata == null) return
                title.value = metadata.description.title?.toString().orEmpty()
                subtitle.value = metadata.description.subtitle?.toString().orEmpty()
                artUrl.value = metadata.description.iconUri?.toString().orEmpty()
            }
        }
        controller?.registerCallback(callback)
        val jobs = listOf(
            AppEventBus.observe<IsPlayingEvent>(scope) { isPlaying.value = it.isPlaying },
            AppEventBus.observe<OnSourceChangeEvent>(scope) {
                title.value = MediaPlayerService.getSongName()
                subtitle.value = MediaPlayerService.getSongArtists()
                artUrl.value = MediaPlayerService.getAlbumArtUrl()
            },
        )

        onDispose {
            controller?.unregisterCallback(callback)
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
        androidx.compose.material3.Text(
            text = title.value,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
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
