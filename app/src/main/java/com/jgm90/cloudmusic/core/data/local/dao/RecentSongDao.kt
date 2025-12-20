package com.jgm90.cloudmusic.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jgm90.cloudmusic.core.data.local.entity.RecentSongEntity

@Dao
interface RecentSongDao {
    @Query("SELECT * FROM recent_songs ORDER BY last_played DESC")
    suspend fun getAll(): List<RecentSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentSongEntity)

    @Query("DELETE FROM recent_songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_songs")
    suspend fun clearAll()
}
