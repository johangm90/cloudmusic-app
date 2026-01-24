package com.jgm90.cloudmusic.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeController {
    private val _seedColor = MutableStateFlow<Color?>(null)
    val seedColor: StateFlow<Color?> = _seedColor.asStateFlow()

    fun setSeedColor(color: Color?) {
        _seedColor.value = color
    }
}
