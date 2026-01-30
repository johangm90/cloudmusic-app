package com.jgm90.cloudmusic.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
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
class AppVersionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.appDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val versionName: StateFlow<String> = dataStore.data
        .map { it[PreferenceKeys.appVersion] ?: "" }
        .stateIn(scope, SharingStarted.Eagerly, "")

    fun setVersion(version: String) {
        scope.launch {
            dataStore.edit { it[PreferenceKeys.appVersion] = version }
        }
    }
}
