package com.jgm90.cloudmusic.feature.playlist.data

import com.jgm90.cloudmusic.core.data.local.dao.SongDao
import com.jgm90.cloudmusic.core.data.local.entity.SongEntity
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

    private fun SongEntity.toModel(): SongModel {
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

    private fun SongModel.toEntity(id: String): SongEntity {
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
}
