package com.jgm90.cloudmusic.feature.playback.presentation.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jgm90.cloudmusic.feature.playback.presentation.component.visualizer.BeatWaveRing

@Composable
fun AlbumArtWithVisualizer(
    coverUrl: String,
    isPlaying: Boolean,
    beatLevel: Float,
    visualizerBands: FloatArray,
    particleColors: List<Color>,
    accentGradient: Brush,
    showVisualizer: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "coverSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000),
            repeatMode = RepeatMode.Restart
        ),
        label = "coverSpinValue"
    )

    val pulse by animateFloatAsState(
        targetValue = 1f + beatLevel.coerceIn(0f, 1f) * 0.18f,
        animationSpec = tween(120),
        label = "beatPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        // Background circle
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                    CircleShape
                )
        )

        // Visualizer ring - only show if enabled
        if (showVisualizer) {
            BeatWaveRing(
                modifier = Modifier.size(320.dp),
                beatLevel = beatLevel,
                isPlaying = isPlaying,
                bands = visualizerBands,
                colors = particleColors,
            )
        }

        // Accent ring
        Box(
            modifier = Modifier
                .size(288.dp)
                .clip(CircleShape)
                .background(accentGradient)
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotation else 0f
                    scaleX = pulse
                    scaleY = pulse
                    alpha = 0.75f + (beatLevel.coerceIn(0f, 1f) * 0.2f)
                }
        )

        // Album art
        AsyncImage(
            model = coverUrl,
            contentDescription = "Album artwork",
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .border(
                    width = (2.dp + (beatLevel * 5f).dp),
                    brush = accentGradient,
                    shape = CircleShape
                )
                .shadow(24.dp, CircleShape)
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotation else 0f
                },
        )
    }
}
