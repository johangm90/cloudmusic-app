package com.jgm90.cloudmusic.feature.playback.presentation.component.visualizer

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val WAVE_LAYERS = 3
private const val POINTS_PER_WAVE = 64

@Composable
fun BeatWaveRing(
    modifier: Modifier,
    beatLevel: Float,
    isPlaying: Boolean,
    bands: FloatArray,
    colors: List<Color>,
) {
    val transition = rememberInfiniteTransition(label = "auroraWave")

    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    val breathe by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val palette = remember(colors) {
        if (colors.size >= 5) colors else listOf(
            Color(0xFF5EEAD4),
            Color(0xFF60A5FA),
            Color(0xFFF59E0B),
            Color(0xFFFB7185),
            Color(0xFFA78BFA),
        )
    }

    val shouldAnimate = isPlaying
    val phase1Value = if (shouldAnimate) phase1 else 0f
    val phase2Value = if (shouldAnimate) phase2 else 0f
    val phase3Value = if (shouldAnimate) phase3 else 0f
    val breatheValue = if (shouldAnimate) breathe else 1f

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.38f
        val waveIntensity = if (isPlaying) 1f else 0.3f
        val effectiveBeat = beatLevel.coerceIn(0f, 1f) * waveIntensity

        // Layer 1: Outer aurora glow
        val outerPath = Path()
        val outerRadius = baseRadius * 1.15f * breatheValue
        val outerAmp = size.minDimension * (0.02f + effectiveBeat * 0.06f)

        for (i in 0..POINTS_PER_WAVE) {
            val angle = (i.toFloat() / POINTS_PER_WAVE) * 2f * PI.toFloat()
            val bandIndex = if (bands.isNotEmpty()) {
                (i * bands.size / POINTS_PER_WAVE).coerceIn(0, bands.size - 1)
            } else i % 8
            val bandLevel = if (bands.isNotEmpty()) bands[bandIndex].coerceIn(0f, 1f) else 0.5f

            val wave1 = sin(angle * 3f + phase1Value) * outerAmp
            val wave2 = sin(angle * 5f + phase2Value * 1.3f) * outerAmp * 0.6f
            val wave3 = cos(angle * 2f + phase3Value) * outerAmp * 0.4f
            val beatWave = sin(angle * 8f) * outerAmp * bandLevel * 1.5f

            val r = outerRadius + wave1 + wave2 + wave3 + beatWave
            val x = center.x + cos(angle) * r
            val y = center.y + sin(angle) * r

            if (i == 0) outerPath.moveTo(x, y) else outerPath.lineTo(x, y)
        }
        outerPath.close()

        // Draw outer glow layers
        for (glowLayer in 3 downTo 1) {
            val glowAlpha = (0.08f + effectiveBeat * 0.05f) / glowLayer
            val glowWidth = size.minDimension * 0.025f * glowLayer
            drawPath(
                path = outerPath,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        palette[0].copy(alpha = glowAlpha),
                        palette[1].copy(alpha = glowAlpha * 0.8f),
                        palette[2].copy(alpha = glowAlpha * 0.6f),
                        palette[0].copy(alpha = glowAlpha),
                    ),
                    center = center
                ),
                style = Stroke(width = glowWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Layer 2: Middle flowing wave
        val middlePath = Path()
        val middleRadius = baseRadius * 1.05f * breatheValue
        val middleAmp = size.minDimension * (0.015f + effectiveBeat * 0.045f)

        for (i in 0..POINTS_PER_WAVE) {
            val angle = (i.toFloat() / POINTS_PER_WAVE) * 2f * PI.toFloat()
            val bandIndex = if (bands.isNotEmpty()) {
                (i * bands.size / POINTS_PER_WAVE).coerceIn(0, bands.size - 1)
            } else i % 8
            val bandLevel = if (bands.isNotEmpty()) bands[bandIndex].coerceIn(0f, 1f) else 0.5f

            val wave1 = sin(angle * 4f - phase1Value * 0.8f) * middleAmp
            val wave2 = cos(angle * 6f + phase2Value) * middleAmp * 0.7f
            val beatWave = sin(angle * 12f + phase3Value) * middleAmp * bandLevel * 2f

            val r = middleRadius + wave1 + wave2 + beatWave
            val x = center.x + cos(angle) * r
            val y = center.y + sin(angle) * r

            if (i == 0) middlePath.moveTo(x, y) else middlePath.lineTo(x, y)
        }
        middlePath.close()

        // Rotating middle layer for dynamic effect
        rotate(degrees = phase1Value * 5f, pivot = center) {
            drawPath(
                path = middlePath,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        palette[1].copy(alpha = 0.4f + effectiveBeat * 0.2f),
                        palette[3].copy(alpha = 0.3f + effectiveBeat * 0.15f),
                        palette[4].copy(alpha = 0.35f + effectiveBeat * 0.18f),
                        palette[1].copy(alpha = 0.4f + effectiveBeat * 0.2f),
                    ),
                    center = center
                ),
                style = Stroke(width = size.minDimension * 0.012f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Layer 3: Inner crisp ring with beat response
        val innerPath = Path()
        val innerRadius = baseRadius * 0.98f
        val innerAmp = size.minDimension * (0.008f + effectiveBeat * 0.025f)

        for (i in 0..POINTS_PER_WAVE) {
            val angle = (i.toFloat() / POINTS_PER_WAVE) * 2f * PI.toFloat()
            val bandIndex = if (bands.isNotEmpty()) {
                (i * bands.size / POINTS_PER_WAVE).coerceIn(0, bands.size - 1)
            } else i % 8
            val bandLevel = if (bands.isNotEmpty()) bands[bandIndex].coerceIn(0f, 1f) else 0.5f

            val wave = sin(angle * 16f + phase1Value * 2f) * innerAmp * (0.5f + bandLevel)

            val r = innerRadius + wave
            val x = center.x + cos(angle) * r
            val y = center.y + sin(angle) * r

            if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
        }
        innerPath.close()

        // Inner ring with gradient
        drawPath(
            path = innerPath,
            brush = Brush.sweepGradient(
                colors = listOf(
                    palette[0].copy(alpha = 0.7f + effectiveBeat * 0.25f),
                    palette[2].copy(alpha = 0.5f + effectiveBeat * 0.2f),
                    palette[4].copy(alpha = 0.6f + effectiveBeat * 0.22f),
                    palette[0].copy(alpha = 0.7f + effectiveBeat * 0.25f),
                ),
                center = center
            ),
            style = Stroke(width = size.minDimension * 0.006f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Beat pulse ring - only visible when beat is strong
        if (effectiveBeat > 0.3f) {
            val pulseRadius = baseRadius * (1.2f + effectiveBeat * 0.15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        palette[0].copy(alpha = (effectiveBeat - 0.3f) * 0.4f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = pulseRadius
                ),
                radius = pulseRadius,
                center = center
            )
        }

        // Sparkle points on strong beats
        if (effectiveBeat > 0.5f && bands.isNotEmpty()) {
            val sparkleCount = 8
            for (i in 0 until sparkleCount) {
                val peakIndex = (i * bands.size / sparkleCount).coerceIn(0, bands.size - 1)
                if (bands[peakIndex] > 0.6f) {
                    val angle = (i.toFloat() / sparkleCount) * 2f * PI.toFloat() + phase1Value
                    val sparkleRadius = baseRadius * 1.1f
                    val x = center.x + cos(angle) * sparkleRadius
                    val y = center.y + sin(angle) * sparkleRadius

                    drawCircle(
                        color = palette[i % palette.size].copy(alpha = bands[peakIndex] * 0.8f),
                        radius = size.minDimension * 0.008f * bands[peakIndex],
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
