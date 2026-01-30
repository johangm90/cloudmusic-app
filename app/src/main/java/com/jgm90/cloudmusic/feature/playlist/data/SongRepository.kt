package com.jgm90.cloudmusic.feature.playlist.data

import com.jgm90.cloudmusic.core.data.local.dao.SongDao
import com.jgm90.cloudmusic.core.data.local.mapper.toEntity
import com.jgm90.cloudmusic.core.data.local.mapper.toModel
import com.jgm90.cloudmusic.core.model.SongModel
import javax.inject.Inject

class SongRepository @Inject constructor(
    private val songDao: SongDao,
) {
    suspend fun getById(id: String): SongModel? {
        return songDao.getById(id)?.toModel()
    }

    suspend fun getAll(): List<SongModel> {
        return songDao.getAll().map { it.toModel() }
    }

    suspend fun getByPlaylist(playlistId: Int): List<SongModel> {
        return songDao.getByPlaylist(playlistId).map { it.toModel() }
    }

    suspend fun insert(model: SongModel) {
        val id = model.id ?: return
        songDao.insert(model.toEntity(id))
    }

    suspend fun update(model: SongModel) {
        val id = model.id ?: return
        songDao.update(model.toEntity(id))
    }

    suspend fun delete(model: SongModel) {
        val id = model.id ?: return
        songDao.delete(model.toEntity(id))
    }

    suspend fun getNextPosition(): Int {
        return (songDao.getMaxPosition() ?: 0) + 1
    }
}
