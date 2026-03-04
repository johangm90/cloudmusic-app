package com.jgm90.cloudmusic.core.model

data class ArtistModel(
    val id: String,
    val name: String,
    val thumbnailUrl: String = "",
    val subtitle: String? = null,
)
