package com.jgm90.cloudmusic.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlist_id: Int = 0,
    val name: String,
    val offline: Int,
)
