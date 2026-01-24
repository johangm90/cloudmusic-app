package com.jgm90.cloudmusic.feature.playback.presentation.component.visualizer

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.jgm90.cloudmusic.feature.playback.presentation.state.AmbientColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val AMBIENT_SHADER = """
    uniform float2 u_resolution;
    uniform float u_time;
    uniform float u_beat;
    uniform float3 u_color_start;
    uniform float3 u_color_mid;
    uniform float3 u_color_end;
    uniform float3 u_accent;

    // Simplex noise function for organic movement
    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453123);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }

    float fbm(float2 p) {
        float value = 0.0;
        float amplitude = 0.5;
        for (int i = 0; i < 4; i++) {
            value += amplitude * noise(p);
            p *= 2.0;
            amplitude *= 0.5;
        }
        return value;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / u_resolution;
        float2 p = uv * 2.0 - 1.0;
        p.x *= u_resolution.x / u_resolution.y;

        // Create flowing nebula effect
        float t = u_time * 0.3;
        float2 q = float2(fbm(p + t * 0.3), fbm(p + float2(1.0)));
        float2 r = float2(fbm(p + q + t * 0.2), fbm(p + q + float2(1.0)));

        float f = fbm(p + r * 2.0);

        // Create depth with multiple layers
        float nebula = f * f * 2.0;
        nebula += fbm(p * 3.0 + t * 0.1) * 0.3;

        // Beat-reactive glow from center
        float centerDist = length(p);
        float pulse = exp(-centerDist * (1.5 - u_beat * 0.5)) * (0.3 + u_beat * 0.4);

        // Smooth color mixing
        float3 baseColor = mix(u_color_start, u_color_mid, uv.y);
        baseColor = mix(baseColor, u_color_end, nebula * 0.5);

        // Add accent color based on noise and beat
        float accentMix = clamp(nebula * 0.4 + pulse * 0.6, 0.0, 1.0);
        float3 col = mix(baseColor, u_accent, accentMix * 0.4);

        // Add subtle vignette
        float vignette = 1.0 - centerDist * 0.3;
        col *= vignette;

        // Beat pulse effect
        col += u_accent * pulse * 0.15;

        // Subtle grain for texture
        float grain = hash(uv * u_time) * 0.02;
        col += grain;

        return half4(clamp(col, 0.0, 1.0), 1.0);
    }
"""

@Composable
fun AmbientBackground(
    ambientColors: AmbientColors,
    beatLevel: Float,
    animate: Boolean,
    useShader: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val canUseShader = useShader && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var bgSize by remember { mutableStateOf(IntSize(1, 1)) }
    var shaderTime by remember { mutableFloatStateOf(0f) }

    val transition = rememberInfiniteTransition(label = "ambientFlow")
    val flowPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowPhase"
    )
    val flowPhaseValue = if (animate) flowPhase else 0f

    val backgroundGradient = remember(ambientColors, flowPhaseValue) {
        val offset1 = Offset(
            0.3f + sin(flowPhaseValue) * 0.2f,
            0.2f + cos(flowPhaseValue * 0.7f) * 0.15f
        )
        val offset2 = Offset(
            0.7f + cos(flowPhaseValue * 0.5f) * 0.2f,
            0.8f + sin(flowPhaseValue * 0.8f) * 0.15f
        )

        Brush.linearGradient(
            0f to ambientColors.backgroundStart,
            0.4f to ambientColors.backgroundMid,
            0.7f to ambientColors.backgroundEnd,
            1f to ambientColors.backgroundStart.copy(alpha = 0.8f),
        )
    }

    val shader = remember {
        if (canUseShader && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(AMBIENT_SHADER)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val shaderBrush = remember(shader) {
        shader?.let { ShaderBrush(it) }
    }

    LaunchedEffect(canUseShader, shader, animate) {
        if (!canUseShader || shader == null || !animate) return@LaunchedEffect
        while (true) {
            withFrameNanos { frameTime ->
                shaderTime = frameTime / 1_000_000_000f
            }
        }
    }

    val shaderReady = canUseShader && bgSize.width > 1 && bgSize.height > 1 && shader != null

    if (shaderReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val beat = if (animate) beatLevel else 0f
        shader?.setFloatUniform("u_resolution", bgSize.width.toFloat(), bgSize.height.toFloat())
        shader?.setFloatUniform("u_time", shaderTime)
        shader?.setFloatUniform("u_beat", beat.coerceIn(0f, 1f))
        shader?.setFloatUniform(
            "u_color_start",
            ambientColors.backgroundStart.red,
            ambientColors.backgroundStart.green,
            ambientColors.backgroundStart.blue
        )
        shader?.setFloatUniform(
            "u_color_mid",
            ambientColors.backgroundMid.red,
            ambientColors.backgroundMid.green,
            ambientColors.backgroundMid.blue
        )
        shader?.setFloatUniform(
            "u_color_end",
            ambientColors.backgroundEnd.red,
            ambientColors.backgroundEnd.green,
            ambientColors.backgroundEnd.blue
        )
        shader?.setFloatUniform(
            "u_accent",
            ambientColors.accentPrimary.red,
            ambientColors.accentPrimary.green,
            ambientColors.accentPrimary.blue
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { bgSize = it }
            .background(
                if (shaderReady && shaderBrush != null) shaderBrush else backgroundGradient
            )
    ) {
        // Fallback animated overlay for non-shader devices
        if (!shaderReady) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Animated gradient orbs
                val orb1Center = Offset(
                    center.x + sin(flowPhase) * size.width * 0.2f,
                    center.y + cos(flowPhase * 0.7f) * size.height * 0.15f
                )
                val orb2Center = Offset(
                    center.x + cos(flowPhase * 0.5f) * size.width * 0.25f,
                    center.y + sin(flowPhase * 0.8f) * size.height * 0.2f
                )

                // Primary orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientColors.accentPrimary.copy(alpha = 0.15f + beatLevel * 0.1f),
                            ambientColors.accentPrimary.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = orb1Center,
                        radius = size.minDimension * 0.5f
                    ),
                    radius = size.minDimension * 0.5f,
                    center = orb1Center
                )

                // Secondary orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientColors.accentSecondary.copy(alpha = 0.1f + beatLevel * 0.08f),
                            ambientColors.accentSecondary.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = orb2Center,
                        radius = size.minDimension * 0.4f
                    ),
                    radius = size.minDimension * 0.4f,
                    center = orb2Center
                )

                // Center glow responsive to beat
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientColors.accentPrimary.copy(alpha = beatLevel * 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.3f * (1f + beatLevel * 0.3f)
                    ),
                    radius = size.minDimension * 0.3f * (1f + beatLevel * 0.3f),
                    center = center
                )
            }
        }

        // Overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF0B1118).copy(alpha = 0.4f)
                        )
                    )
                )
        )

        content()
    }
}
