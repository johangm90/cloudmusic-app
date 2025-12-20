package com.jgm90.cloudmusic.core.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromArtistList(artists: List<String>?): String {
        return artists?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toArtistList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
