package com.jgm90.cloudmusic.feature.playlist.data

import android.content.Context
import com.jgm90.cloudmusic.core.data.local.CloudMusicDatabase
import com.jgm90.cloudmusic.core.data.local.dao.PlaylistWithCount
import com.jgm90.cloudmusic.core.data.local.entity.PlaylistEntity
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel

class PlaylistData(context: Context) {
    private val playlistDao = CloudMusicDatabase.getInstance(context).playlistDao()

    suspend fun getById(id: Int): PlaylistModel? {
        return playlistDao.getById(id)?.toModel()
    }

    suspend fun getAll(): List<PlaylistModel> {
        return playlistDao.getAllWithCount().map { it.toModel() }
    }

    suspend fun insert(obj: PlaylistModel) {
        playlistDao.insert(obj.toEntity())
    }

    suspend fun update(obj: PlaylistModel) {
        playlistDao.update(obj.toEntity())
    }

    suspend fun delete(obj: PlaylistModel) {
        playlistDao.deleteSongsForPlaylist(obj.playlist_id)
        playlistDao.delete(obj.toEntity())
    }

    private fun PlaylistEntity.toModel(): PlaylistModel {
        return PlaylistModel(
            playlist_id = playlist_id,
            name = name,
            song_count = 0,
            offline = offline,
        )
    }

    private fun PlaylistWithCount.toModel(): PlaylistModel {
        return PlaylistModel(
            playlist_id = playlist_id,
            name = name,
            song_count = song_count,
            offline = offline,
        )
    }

    private fun PlaylistModel.toEntity(): PlaylistEntity {
        return PlaylistEntity(
            playlist_id = playlist_id,
            name = name,
            offline = offline,
        )
    }
}
