package com.jgm90.cloudmusic.feature.search.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.jgm90.cloudmusic.core.data.preferences.PreferenceKeys
import com.jgm90.cloudmusic.core.data.preferences.appDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SearchHistoryStore(context: Context) {
    private val dataStore = context.appDataStore
    private val gson = Gson()

    suspend fun getHistory(): List<String> {
        val json = dataStore.data
            .map { it[PreferenceKeys.searchHistory] }
            .first()
            ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    suspend fun addQuery(query: String, maxItems: Int = 10) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val list = getHistory().toMutableList()
        list.removeAll { it.equals(trimmed, ignoreCase = true) }
        list.add(0, trimmed)
        if (list.size > maxItems) {
            list.subList(maxItems, list.size).clear()
        }
        saveList(list)
    }

    suspend fun clear() {
        dataStore.edit { it.remove(PreferenceKeys.searchHistory) }
    }

    private suspend fun saveList(list: List<String>) {
        dataStore.edit { it[PreferenceKeys.searchHistory] = gson.toJson(list) }
    }
}
