package com.jgm90.cloudmusic.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jgm90.cloudmusic.core.data.local.entity.PlaylistEntity

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE playlist_id = :id")
    suspend fun getById(id: Int): PlaylistEntity?

    @Query(
        "SELECT p.playlist_id AS playlist_id, p.name AS name, p.offline AS offline, " +
            "COUNT(s.id) AS song_count " +
            "FROM playlists p LEFT JOIN songs s ON s.playlist_id = p.playlist_id " +
            "GROUP BY p.playlist_id"
    )
    suspend fun getAllWithCount(): List<PlaylistWithCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaylistEntity): Long

    @Update
    suspend fun update(entity: PlaylistEntity)

    @Delete
    suspend fun delete(entity: PlaylistEntity)

    @Query("DELETE FROM songs WHERE playlist_id = :playlistId")
    suspend fun deleteSongsForPlaylist(playlistId: Int)
}
