package com.jgm90.cloudmusic.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val artist: List<String> = emptyList(),
    val album: String,
    val pic_id: String?,
    val url_id: String?,
    val lyric_id: String?,
    val source: String?,
    val local_file: String?,
    val local_thumbnail: String?,
    val local_lyric: String?,
    val position: Int,
    val position_date: String?,
    val playlist_id: Int,
)
