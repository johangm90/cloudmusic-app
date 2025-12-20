package com.jgm90.cloudmusic.core.data.local.dao

data class PlaylistWithCount(
    val playlist_id: Int,
    val name: String,
    val offline: Int,
    val song_count: Int,
)
