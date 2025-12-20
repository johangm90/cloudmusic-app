package com.jgm90.cloudmusic.feature.playlist.data

import com.jgm90.cloudmusic.core.data.local.dao.PlaylistDao
import com.jgm90.cloudmusic.core.data.local.dao.PlaylistWithCount
import com.jgm90.cloudmusic.core.data.local.entity.PlaylistEntity
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
