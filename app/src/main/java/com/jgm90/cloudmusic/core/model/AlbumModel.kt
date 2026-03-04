package com.jgm90.cloudmusic.core.model

data class AlbumModel(
    val id: String,
    val name: String,
    val artistName: String = "",
    val thumbnailUrl: String = "",
)
