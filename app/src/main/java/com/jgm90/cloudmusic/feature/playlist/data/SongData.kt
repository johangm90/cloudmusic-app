package com.jgm90.cloudmusic.feature.playlist.data

import android.content.Context
import com.jgm90.cloudmusic.core.data.local.CloudMusicDatabase
import com.jgm90.cloudmusic.core.data.local.entity.SongEntity
import com.jgm90.cloudmusic.core.model.SongModel

class SongData(context: Context) {
    private val songDao = CloudMusicDatabase.getInstance(context).songDao()

    suspend fun getById(id: String): SongModel? {
        return songDao.getById(id)?.toModel()
    }

    suspend fun getAll(): List<SongModel> {
        return songDao.getAll().map { it.toModel() }
    }

    suspend fun getAllByPlaylist(playlistId: Int): List<SongModel> {
        return songDao.getByPlaylist(playlistId).map { it.toModel() }
    }

    suspend fun insert(obj: SongModel) {
        val id = obj.id ?: return
        songDao.insert(obj.toEntity(id))
    }

    suspend fun update(obj: SongModel) {
        val id = obj.id ?: return
        songDao.update(obj.toEntity(id))
    }

    suspend fun delete(obj: SongModel) {
        val id = obj.id ?: return
        songDao.delete(obj.toEntity(id))
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
