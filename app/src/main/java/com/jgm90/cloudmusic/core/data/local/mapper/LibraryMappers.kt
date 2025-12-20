package com.jgm90.cloudmusic.core.data.local.mapper

import com.jgm90.cloudmusic.core.data.local.entity.LikedSongEntity
import com.jgm90.cloudmusic.core.data.local.entity.RecentSongEntity
import com.jgm90.cloudmusic.core.model.SongModel

fun SongModel.toRecentEntity(timestamp: Long): RecentSongEntity {
    val songId = id ?: ""
    return RecentSongEntity(
        id = songId,
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
        last_played = timestamp,
    )
}

fun SongModel.toLikedEntity(timestamp: Long): LikedSongEntity {
    val songId = id ?: ""
    return LikedSongEntity(
        id = songId,
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
        liked_at = timestamp,
    )
}

fun RecentSongEntity.toModel(): SongModel {
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
        position = 0,
        position_date = null,
        playlist_id = 0,
    )
}

fun LikedSongEntity.toModel(): SongModel {
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
        position = 0,
        position_date = null,
        playlist_id = 0,
    )
}
