package com.jgm90.cloudmusic.feature.playback.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.playback.PlaybackMode

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: PlaybackMode,
    accentColor: Color,
    isLoading: Boolean,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                RoundedCornerShape(32.dp)
            )
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onShuffle,
            enabled = enabled,
            modifier = Modifier.alpha(if (shuffleEnabled) 1f else 0.4f)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle_black_24dp),
                contentDescription = "Shuffle",
                tint = Color.White,
            )
        }
        IconButton(
            onClick = onPrevious,
            enabled = enabled,
            modifier = Modifier
                .size(46.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    CircleShape
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_previous),
                contentDescription = "Previous",
                tint = Color.White,
            )
        }
        IconButton(
            onClick = onPlayPause,
            enabled = enabled,
            modifier = Modifier
                .size(64.dp)
                .background(accentColor, CircleShape)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        IconButton(
            onClick = onNext,
            enabled = enabled,
            modifier = Modifier
                .size(46.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    CircleShape
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = "Next",
                tint = Color.White,
            )
        }
        IconButton(onClick = onRepeat, enabled = enabled) {
            val iconRes = when (repeatMode) {
                PlaybackMode.NORMAL, PlaybackMode.REPEAT -> R.drawable.ic_repeat_black_24dp
                PlaybackMode.REPEAT_ONE -> R.drawable.ic_repeat_one_black_24dp
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = "Repeat mode",
                tint = Color.White,
            )
        }
    }
}
