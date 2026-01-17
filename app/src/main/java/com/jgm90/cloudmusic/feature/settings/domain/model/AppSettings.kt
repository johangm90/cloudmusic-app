package com.jgm90.cloudmusic.feature.settings.domain.model

import androidx.compose.runtime.Immutable

enum class VisualizerStyle {
    WAVE_RING,
    NONE,
}

enum class ParticleLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}

enum class ShaderQuality {
    LOW,
    MEDIUM,
    HIGH,
}

@Immutable
data class AppSettings(
    val ambientModeEnabled: Boolean = true,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.WAVE_RING,
    val particleLevel: ParticleLevel = ParticleLevel.MEDIUM,
    val shaderQuality: ShaderQuality = ShaderQuality.MEDIUM,
)
