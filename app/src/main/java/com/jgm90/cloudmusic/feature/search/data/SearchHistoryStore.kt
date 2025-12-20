package com.jgm90.cloudmusic.feature.search.data

import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryStore(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = Gson()

    fun getHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addQuery(query: String, maxItems: Int = 10) {
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

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveList(list: List<String>) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
    }

    private companion object {
        const val KEY_HISTORY = "search_history"
    }
}
