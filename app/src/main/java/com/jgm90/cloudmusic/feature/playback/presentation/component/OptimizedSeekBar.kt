package com.jgm90.cloudmusic.feature.playback.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun OptimizedSeekBar(
    progress: Int,
    max: Int,
    onSeekStart: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onSeekEnd: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val sizeState = remember { mutableStateOf(IntSize(1, 1)) }
    val fraction = if (max > 0) progress.toFloat() / max else 0f
    val clamped = fraction.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .onSizeChanged { sizeState.value = it }
            .let { base ->
                if (!enabled) {
                    base
                } else {
                    base.pointerInput(max) {
                        detectDragGestures(
                            onDragStart = { onSeekStart() },
                            onDragEnd = { onSeekEnd() },
                            onDragCancel = { onSeekEnd() },
                        ) { change, _ ->
                            change.consume()
                            val x = change.position.x.coerceIn(0f, sizeState.value.width.toFloat())
                            val newValue = ((x / sizeState.value.width) * max).toInt()
                            onSeekChange(newValue)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = sizeState.value.height * 0.18f
            val centerY = sizeState.value.height / 2f
            val start = Offset(0f, centerY)
            val end = Offset(sizeState.value.width.toFloat(), centerY)
            val progressX = sizeState.value.width * clamped

            drawLine(
                color = inactiveColor,
                start = start,
                end = end,
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )

            drawLine(
                color = activeColor,
                start = start,
                end = Offset(progressX, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )

            drawCircle(
                color = activeColor,
                radius = trackHeight * 1.6f,
                center = Offset(progressX, centerY),
            )
        }
    }
}
