package com.jgm90.cloudmusic.feature.settings.data

import android.app.ActivityManager
import android.content.Context
import android.preference.PreferenceManager
import com.jgm90.cloudmusic.feature.settings.domain.model.AppSettings
import com.jgm90.cloudmusic.feature.settings.domain.model.ParticleLevel
import com.jgm90.cloudmusic.feature.settings.domain.model.ShaderQuality
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
        val isLowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .isLowRamDevice
        val defaults = if (isLowRam) {
            AppSettings(
                ambientModeEnabled = false,
                visualizerStyle = VisualizerStyle.NONE,
                particleLevel = ParticleLevel.NONE,
                shaderQuality = ShaderQuality.LOW,
            )
        } else {
            AppSettings(
                ambientModeEnabled = true,
                visualizerStyle = VisualizerStyle.WAVE_RING,
                particleLevel = ParticleLevel.MEDIUM,
                shaderQuality = ShaderQuality.MEDIUM,
            )
        }
        return AppSettings(
            ambientModeEnabled = prefs.getBoolean(KEY_AMBIENT_MODE, defaults.ambientModeEnabled),
            visualizerStyle = VisualizerStyle.entries.getOrElse(
                prefs.getInt(KEY_VISUALIZER_STYLE, defaults.visualizerStyle.ordinal)
            ) { defaults.visualizerStyle },
            particleLevel = ParticleLevel.entries.getOrElse(
                prefs.getInt(KEY_PARTICLE_LEVEL, defaults.particleLevel.ordinal)
            ) { defaults.particleLevel },
            shaderQuality = ShaderQuality.entries.getOrElse(
                prefs.getInt(KEY_SHADER_QUALITY, defaults.shaderQuality.ordinal)
            ) { defaults.shaderQuality },
        )
    }

    fun updateAmbientMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMBIENT_MODE, enabled).apply()
        _settings.value = _settings.value.copy(ambientModeEnabled = enabled)
    }

    fun updateVisualizerStyle(style: VisualizerStyle) {
        prefs.edit().putInt(KEY_VISUALIZER_STYLE, style.ordinal).apply()
        _settings.value = _settings.value.copy(visualizerStyle = style)
    }

    fun updateParticleLevel(level: ParticleLevel) {
        prefs.edit().putInt(KEY_PARTICLE_LEVEL, level.ordinal).apply()
        _settings.value = _settings.value.copy(particleLevel = level)
    }

    fun updateShaderQuality(quality: ShaderQuality) {
        prefs.edit().putInt(KEY_SHADER_QUALITY, quality.ordinal).apply()
        _settings.value = _settings.value.copy(shaderQuality = quality)
    }

    companion object {
        private const val KEY_AMBIENT_MODE = "settings_ambient_mode"
        private const val KEY_VISUALIZER_STYLE = "settings_visualizer_style"
        private const val KEY_PARTICLE_LEVEL = "settings_particle_level"
        private const val KEY_SHADER_QUALITY = "settings_shader_quality"
    }
}
