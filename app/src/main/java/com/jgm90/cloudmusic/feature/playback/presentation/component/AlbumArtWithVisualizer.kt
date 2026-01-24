package com.jgm90.cloudmusic.feature.playback.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jgm90.cloudmusic.feature.playback.presentation.component.visualizer.BeatWaveRing

@Composable
fun AlbumArtWithVisualizer(
    coverUrl: String,
    isPlaying: Boolean,
    beatLevel: Float,
    visualizerBands: FloatArray,
    showVisualizer: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val ringColors = listOf(
        accentColor,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        accentColor,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                    CircleShape
                )
        )

        if (showVisualizer) {
            BeatWaveRing(
                modifier = Modifier.size(320.dp),
                beatLevel = beatLevel,
                isPlaying = isPlaying,
                bands = visualizerBands,
                colors = ringColors,
            )
        }

        AsyncImage(
            model = coverUrl,
            contentDescription = "Album artwork",
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = accentColor,
                    shape = CircleShape
                ),
        )
    }
}
