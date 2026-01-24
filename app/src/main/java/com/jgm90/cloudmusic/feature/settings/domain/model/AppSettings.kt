package com.jgm90.cloudmusic.feature.settings.domain.model

import androidx.compose.runtime.Immutable

enum class VisualizerStyle {
    WAVE_RING,
    NONE,
}

@Immutable
data class AppSettings(
    val visualizerStyle: VisualizerStyle = VisualizerStyle.NONE,
)
