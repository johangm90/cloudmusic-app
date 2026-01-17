package com.jgm90.cloudmusic.feature.playback.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun OptimizedSeekBar(
    progress: Int,
    max: Int,
    onSeekStart: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onSeekEnd: () -> Unit,
    activeBrush: Brush,
    inactiveColor: Color,
    beatLevel: Float,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize(1, 1)) }
    val fraction = if (max > 0) progress.toFloat() / max else 0f
    val clamped = fraction.coerceIn(0f, 1f)
    val thumbPulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .onSizeChanged { size = it }
            .pointerInput(max) {
                detectDragGestures(
                    onDragStart = { onSeekStart() },
                    onDragEnd = { onSeekEnd() },
                    onDragCancel = { onSeekEnd() },
                ) { change, _ ->
                    change.consume()
                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                    val newValue = ((x / size.width) * max).toInt()
                    onSeekChange(newValue)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = size.height * 0.18f
            val centerY = size.height / 2f
            val start = Offset(0f, centerY)
            val end = Offset(size.width.toFloat(), centerY)
            val progressX = size.width * clamped

            // Inactive track
            drawLine(
                color = inactiveColor,
                start = start,
                end = end,
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )

            // Active track
            drawLine(
                brush = activeBrush,
                start = start,
                end = Offset(progressX, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )

            // Optimized wave - reduced from 40 to 10 segments using Path batching
            val waveAmp = trackHeight * (1.2f + beatLevel.coerceIn(0f, 1f) * 2.2f)
            val waveSteps = 10
            val waveStep = size.width.toFloat() / waveSteps

            val wavePath = Path()
            for (i in 0..waveSteps) {
                val x = i * waveStep
                val t = x / size.width.toFloat()
                val y = centerY + sin((t * 10f + clamped * 4f) * PI.toFloat()) * waveAmp * 0.2f
                if (i == 0) {
                    wavePath.moveTo(x, y)
                } else {
                    wavePath.lineTo(x, y)
                }
            }

            drawPath(
                path = wavePath,
                brush = activeBrush,
                style = Stroke(width = trackHeight * 0.35f, cap = StrokeCap.Round),
                alpha = 0.25f
            )

            // Thumb glow
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = trackHeight * 2.2f,
                center = Offset(progressX, centerY),
            )

            // Thumb
            drawCircle(
                brush = activeBrush,
                radius = trackHeight * 1.6f * thumbPulse,
                center = Offset(progressX, centerY),
            )
        }
    }
}
