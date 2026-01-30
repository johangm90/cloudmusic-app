package com.jgm90.cloudmusic.core.data.local.mapper

import com.jgm90.cloudmusic.core.data.local.entity.SongEntity
import com.jgm90.cloudmusic.core.model.SongModel

fun SongEntity.toModel(): SongModel {
    return SongModel(
        id = id,
        name = name,
        artist = artist,
        album = album,
        pic_id = pic_id,
        url_id = url_id,
        lyric_id = lyric_id,
        source = source,
        local_file = local_file,
        local_thumbnail = local_thumbnail,
        local_lyric = local_lyric,
        position = position,
        position_date = position_date,
        playlist_id = playlist_id,
    )
}

fun SongModel.toEntity(id: String): SongEntity {
    return SongEntity(
        id = id,
        name = name,
        artist = artist,
        album = album,
        pic_id = pic_id,
        url_id = url_id,
        lyric_id = lyric_id,
        source = source,
        local_file = local_file,
        local_thumbnail = local_thumbnail,
        local_lyric = local_lyric,
        position = position,
        position_date = position_date,
        playlist_id = playlist_id,
    )
}
