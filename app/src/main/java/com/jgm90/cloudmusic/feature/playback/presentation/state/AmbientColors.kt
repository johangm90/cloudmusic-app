package com.jgm90.cloudmusic.feature.playback.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AmbientColors(
    val backgroundStart: Color,
    val backgroundMid: Color,
    val backgroundEnd: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val particleColors: List<Color>,
) {
    companion object {
        val Default = AmbientColors(
            backgroundStart = Color(0xFF0B0F14),
            backgroundMid = Color(0xFF12202C),
            backgroundEnd = Color(0xFF0B2B2A),
            accentPrimary = Color(0xFF5EEAD4),
            accentSecondary = Color(0xFFF59E0B),
            particleColors = listOf(
                Color(0xFF5EEAD4),
                Color(0xFF60A5FA),
                Color(0xFFF59E0B),
                Color(0xFFFB7185),
                Color(0xFFA78BFA),
            )
        )
    }
}
