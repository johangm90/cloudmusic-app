package com.jgm90.cloudmusic.feature.playlist.data

import com.jgm90.cloudmusic.core.data.local.dao.PlaylistDao
import com.jgm90.cloudmusic.core.data.local.mapper.toEntity
import com.jgm90.cloudmusic.core.data.local.mapper.toModel
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import javax.inject.Inject

class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
) {
    suspend fun getById(id: Int): PlaylistModel? {
        return playlistDao.getById(id)?.toModel()
    }

    suspend fun getAll(): List<PlaylistModel> {
        return playlistDao.getAllWithCount().map { it.toModel() }
    }

    suspend fun insert(model: PlaylistModel) {
        playlistDao.insert(model.toEntity())
    }

    suspend fun update(model: PlaylistModel) {
        playlistDao.update(model.toEntity())
    }

    suspend fun delete(model: PlaylistModel) {
        playlistDao.deleteSongsForPlaylist(model.playlist_id)
        playlistDao.delete(model.toEntity())
    }
}
