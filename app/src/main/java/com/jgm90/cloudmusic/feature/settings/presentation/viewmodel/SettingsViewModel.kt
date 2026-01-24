package com.jgm90.cloudmusic.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.jgm90.cloudmusic.feature.settings.data.SettingsRepository
import com.jgm90.cloudmusic.feature.settings.domain.model.AppSettings
import com.jgm90.cloudmusic.feature.settings.domain.model.VisualizerStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    fun setVisualizerStyle(style: VisualizerStyle) {
        settingsRepository.updateVisualizerStyle(style)
    }
}
