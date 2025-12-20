package com.jgm90.cloudmusic.core.data.local.repository

import com.jgm90.cloudmusic.core.data.local.dao.LikedSongDao
import com.jgm90.cloudmusic.core.data.local.dao.RecentSongDao
import com.jgm90.cloudmusic.core.data.local.mapper.toLikedEntity
import com.jgm90.cloudmusic.core.data.local.mapper.toModel
import com.jgm90.cloudmusic.core.data.local.mapper.toRecentEntity
import com.jgm90.cloudmusic.core.model.SongModel
import javax.inject.Inject

class LibraryRepository @Inject constructor(
    private val recentSongDao: RecentSongDao,
    private val likedSongDao: LikedSongDao,
) {
    suspend fun addRecent(song: SongModel) {
        song.id ?: return
        recentSongDao.insert(song.toRecentEntity(System.currentTimeMillis()))
    }

    suspend fun getRecent(): List<SongModel> {
        return recentSongDao.getAll().map { it.toModel() }
    }

    suspend fun getLiked(): List<SongModel> {
        return likedSongDao.getAll().map { it.toModel() }
    }

    suspend fun isLiked(songId: String?): Boolean {
        val id = songId ?: return false
        return likedSongDao.exists(id) > 0
    }

    suspend fun toggleLiked(song: SongModel): Boolean {
        val songId = song.id ?: return false
        val alreadyLiked = likedSongDao.exists(songId) > 0
        return if (alreadyLiked) {
            likedSongDao.deleteById(songId)
            false
        } else {
            likedSongDao.insert(song.toLikedEntity(System.currentTimeMillis()))
            true
        }
    }
}
