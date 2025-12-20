package com.jgm90.cloudmusic.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jgm90.cloudmusic.core.data.local.entity.LikedSongEntity

@Dao
interface LikedSongDao {
    @Query("SELECT * FROM liked_songs ORDER BY liked_at DESC")
    suspend fun getAll(): List<LikedSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM liked_songs WHERE id = :id")
    suspend fun exists(id: String): Int
}
