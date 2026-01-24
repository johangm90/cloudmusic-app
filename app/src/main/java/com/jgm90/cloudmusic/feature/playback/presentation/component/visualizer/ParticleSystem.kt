package com.jgm90.cloudmusic.feature.playback.presentation.component.visualizer

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val ORBIT_COUNT = 3

@Immutable
private data class FloatingParticle(
    val orbitRadius: Float,
    val orbitSpeed: Float,
    val startAngle: Float,
    val size: Float,
    val pulseSpeed: Float,
    val pulsePhase: Float,
    val colorIndex: Int,
    val verticalOffset: Float,
)

@Composable
fun ParticleSystem(
    modifier: Modifier,
    intensity: Float,
    enabled: Boolean,
    particleCount: Int,
    colors: List<Color>,
) {
    val particles = remember(particleCount) {
        List(particleCount) { index ->
            val orbit = index % ORBIT_COUNT
            FloatingParticle(
                orbitRadius = 0.35f + orbit * 0.15f + Random.nextFloat() * 0.08f,
                orbitSpeed = (0.3f + Random.nextFloat() * 0.4f) * if (Random.nextBoolean()) 1f else -1f,
                startAngle = Random.nextFloat() * 2f * PI.toFloat(),
                size = 0.004f + Random.nextFloat() * 0.006f,
                pulseSpeed = 0.5f + Random.nextFloat() * 1.5f,
                pulsePhase = Random.nextFloat() * 2f * PI.toFloat(),
                colorIndex = Random.nextInt(5),
                verticalOffset = (Random.nextFloat() - 0.5f) * 0.1f,
            )
        }
    }

    val palette = remember(colors) {
        if (colors.size >= 5) colors else listOf(
            Color(0xFF5EEAD4),
            Color(0xFF60A5FA),
            Color(0xFFF59E0B),
            Color(0xFFFB7185),
            Color(0xFFA78BFA),
        )
    }

    val transition = rememberInfiniteTransition(label = "floatingParticles")

    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitTime"
    )

    val pulseTime by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseTime"
    )

    val driftY by transition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    )

    Canvas(modifier = modifier) {
        if (!enabled || intensity <= 0f) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension
        val effectiveIntensity = intensity.coerceIn(0f, 1f)
        val orbitTime = time
        val pulse = pulseTime
        val drift = driftY

        // Draw connection lines between nearby particles (subtle web effect)
        if (effectiveIntensity > 0.2f) {
            val positions = particles.map { p ->
                val angle = p.startAngle + orbitTime * p.orbitSpeed
                val r = base * p.orbitRadius * (1f + effectiveIntensity * 0.15f)
                val yOffset = base * (p.verticalOffset + drift)
                Offset(
                    center.x + cos(angle) * r,
                    center.y + sin(angle) * r + yOffset
                )
            }

            for (i in positions.indices) {
                for (j in i + 1 until positions.size) {
                    val dist = (positions[i] - positions[j]).getDistance()
                    if (dist < base * 0.15f) {
                        val alpha = ((1f - dist / (base * 0.15f)) * 0.15f * effectiveIntensity)
                        drawLine(
                            color = palette[particles[i].colorIndex % palette.size].copy(alpha = alpha),
                            start = positions[i],
                            end = positions[j],
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }

        // Draw particles with glow
        for (p in particles) {
            val angle = p.startAngle + orbitTime * p.orbitSpeed
            val r = base * p.orbitRadius * (1f + effectiveIntensity * 0.15f)
            val yOffset = base * (p.verticalOffset + drift)
            val x = center.x + cos(angle) * r
            val y = center.y + sin(angle) * r + yOffset

            val pulseWave = sin(pulse * p.pulseSpeed + p.pulsePhase)
            val sizeMult = 1f + pulseWave * 0.3f + effectiveIntensity * 0.5f
            val particleSize = base * p.size * sizeMult

            val baseAlpha = 0.3f + effectiveIntensity * 0.5f
            val alpha = baseAlpha * (0.7f + pulseWave * 0.3f)

            val particleColor = palette[p.colorIndex % palette.size]

            // Outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particleColor.copy(alpha = alpha * 0.4f),
                        particleColor.copy(alpha = alpha * 0.1f),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = particleSize * 3f
                ),
                radius = particleSize * 3f,
                center = Offset(x, y)
            )

            // Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha * 0.9f),
                        particleColor.copy(alpha = alpha),
                        particleColor.copy(alpha = alpha * 0.5f),
                    ),
                    center = Offset(x, y),
                    radius = particleSize
                ),
                radius = particleSize,
                center = Offset(x, y)
            )
        }

        // Add some stationary twinkling stars in the background
        if (effectiveIntensity > 0.1f) {
            val starCount = 12
            for (i in 0 until starCount) {
                val starAngle = (i.toFloat() / starCount) * 2f * PI.toFloat()
                val starRadius = base * (0.55f + (i % 3) * 0.05f)
                val twinkle = sin(pulse * 2f + i.toFloat())
                val starAlpha = (0.1f + twinkle * 0.1f) * effectiveIntensity

                if (starAlpha > 0.05f) {
                    val starX = center.x + cos(starAngle) * starRadius
                    val starY = center.y + sin(starAngle) * starRadius

                    drawCircle(
                        color = Color.White.copy(alpha = starAlpha),
                        radius = base * 0.002f * (1f + twinkle * 0.5f),
                        center = Offset(starX, starY)
                    )
                }
            }
        }
    }
}
