package com.jgm90.cloudmusic.core.data.preferences

import android.content.Context
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.appDataStore by preferencesDataStore(
    name = "cloudmusic_prefs",
    produceMigrations = { context ->
        val legacyName = "${context.packageName}_preferences"
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = legacyName,
                keysToMigrate = setOf(
                    "SHUFFLE",
                    "MODE",
                    "VERSION",
                    "settings_visualizer_style",
                    "search_history"
                ),
                migrate = { _, current -> current }
            )
        )
    }
)

object PreferenceKeys {
    val playbackShuffle = booleanPreferencesKey("SHUFFLE")
    val playbackRepeatMode = intPreferencesKey("MODE")
    val appVersion = stringPreferencesKey("VERSION")
    val visualizerStyle = intPreferencesKey("settings_visualizer_style")
    val searchHistory = stringPreferencesKey("search_history")
}
