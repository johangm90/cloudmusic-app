package com.jgm90.cloudmusic.feature.settings.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.jgm90.cloudmusic.core.data.preferences.PreferenceKeys
import com.jgm90.cloudmusic.core.data.preferences.appDataStore
import com.jgm90.cloudmusic.feature.settings.domain.model.AppSettings
import com.jgm90.cloudmusic.feature.settings.domain.model.VisualizerStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dataStore = context.appDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings: StateFlow<AppSettings> = dataStore.data
        .map { prefs ->
            val styleIndex = prefs[PreferenceKeys.visualizerStyle] ?: VisualizerStyle.NONE.ordinal
            AppSettings(
                visualizerStyle = VisualizerStyle.entries.getOrElse(styleIndex) {
                    VisualizerStyle.NONE
                },
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings(visualizerStyle = VisualizerStyle.NONE))

    fun updateVisualizerStyle(style: VisualizerStyle) {
        scope.launch {
            dataStore.edit { it[PreferenceKeys.visualizerStyle] = style.ordinal }
        }
    }
}
