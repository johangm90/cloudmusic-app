package com.jgm90.cloudmusic.core.playback

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.jgm90.cloudmusic.core.data.preferences.PreferenceKeys
import com.jgm90.cloudmusic.core.data.preferences.appDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class PlaybackPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.appDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val shuffleEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[PreferenceKeys.playbackShuffle] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val repeatMode: StateFlow<PlaybackMode> = dataStore.data
        .map { prefs ->
            val raw = prefs[PreferenceKeys.playbackRepeatMode] ?: PlaybackMode.NORMAL.ordinal
            PlaybackMode.entries.getOrElse(raw) { PlaybackMode.NORMAL }
        }
        .stateIn(scope, SharingStarted.Eagerly, PlaybackMode.NORMAL)

    fun setShuffle(enabled: Boolean) {
        scope.launch {
            dataStore.edit { it[PreferenceKeys.playbackShuffle] = enabled }
        }
    }

    fun setRepeatMode(mode: PlaybackMode) {
        scope.launch {
            dataStore.edit { it[PreferenceKeys.playbackRepeatMode] = mode.ordinal }
        }
    }
}
