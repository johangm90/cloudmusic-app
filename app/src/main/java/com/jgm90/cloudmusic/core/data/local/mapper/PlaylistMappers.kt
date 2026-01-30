package com.jgm90.cloudmusic.core.data.local.mapper

import com.jgm90.cloudmusic.core.data.local.dao.PlaylistWithCount
import com.jgm90.cloudmusic.core.data.local.entity.PlaylistEntity
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel

fun PlaylistEntity.toModel(): PlaylistModel {
    return PlaylistModel(
        playlist_id = playlist_id,
        name = name,
        song_count = 0,
        offline = offline,
    )
}

fun PlaylistWithCount.toModel(): PlaylistModel {
    return PlaylistModel(
        playlist_id = playlist_id,
        name = name,
        song_count = song_count,
        offline = offline,
    )
}

fun PlaylistModel.toEntity(): PlaylistEntity {
    return PlaylistEntity(
        playlist_id = playlist_id,
        name = name,
        offline = offline,
    )
}
