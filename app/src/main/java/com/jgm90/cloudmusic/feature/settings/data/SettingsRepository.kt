package com.jgm90.cloudmusic.feature.settings.data

import android.content.Context
import android.preference.PreferenceManager
import com.jgm90.cloudmusic.feature.settings.domain.model.AppSettings
import com.jgm90.cloudmusic.feature.settings.domain.model.VisualizerStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            visualizerStyle = VisualizerStyle.entries.getOrElse(
                prefs.getInt(KEY_VISUALIZER_STYLE, VisualizerStyle.NONE.ordinal)
            ) { VisualizerStyle.NONE },
        )
    }

    fun updateVisualizerStyle(style: VisualizerStyle) {
        prefs.edit().putInt(KEY_VISUALIZER_STYLE, style.ordinal).apply()
        _settings.value = _settings.value.copy(visualizerStyle = style)
    }

    companion object {
        private const val KEY_VISUALIZER_STYLE = "settings_visualizer_style"
    }
}
