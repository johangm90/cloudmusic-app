package com.jgm90.cloudmusic.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.jgm90.cloudmusic.feature.settings.data.SettingsRepository
import com.jgm90.cloudmusic.feature.settings.domain.model.AppSettings
import com.jgm90.cloudmusic.feature.settings.domain.model.ParticleLevel
import com.jgm90.cloudmusic.feature.settings.domain.model.ShaderQuality
import com.jgm90.cloudmusic.feature.settings.domain.model.VisualizerStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    fun setAmbientMode(enabled: Boolean) {
        settingsRepository.updateAmbientMode(enabled)
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        settingsRepository.updateVisualizerStyle(style)
    }

    fun setParticleLevel(level: ParticleLevel) {
        settingsRepository.updateParticleLevel(level)
    }

    fun setShaderQuality(quality: ShaderQuality) {
        settingsRepository.updateShaderQuality(quality)
    }
}
