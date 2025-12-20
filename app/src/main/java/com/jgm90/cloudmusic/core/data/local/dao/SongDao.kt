package com.jgm90.cloudmusic.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jgm90.cloudmusic.core.data.local.entity.SongEntity

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE playlist_id = :playlistId ORDER BY position, position_date")
    suspend fun getByPlaylist(playlistId: Int): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SongEntity)

    @Update
    suspend fun update(entity: SongEntity)

    @Delete
    suspend fun delete(entity: SongEntity)

    @Query("SELECT MAX(position) FROM songs")
    suspend fun getMaxPosition(): Int?
}
