package com.jgm90.cloudmusic.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jgm90.cloudmusic.core.data.local.dao.PlaylistDao
import com.jgm90.cloudmusic.core.data.local.dao.LikedSongDao
import com.jgm90.cloudmusic.core.data.local.dao.RecentSongDao
import com.jgm90.cloudmusic.core.data.local.dao.SongDao
import com.jgm90.cloudmusic.core.data.local.entity.LikedSongEntity
import com.jgm90.cloudmusic.core.data.local.entity.PlaylistEntity
import com.jgm90.cloudmusic.core.data.local.entity.RecentSongEntity
import com.jgm90.cloudmusic.core.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        RecentSongEntity::class,
        LikedSongEntity::class,
    ],
    version = 3,
)
@TypeConverters(Converters::class)
abstract class CloudMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentSongDao(): RecentSongDao
    abstract fun likedSongDao(): LikedSongDao

    companion object {
        private const val DB_NAME = "cloudmusic.sqlite"
        @Volatile
        private var instance: CloudMusicDatabase? = null

        fun getInstance(context: Context): CloudMusicDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CloudMusicDatabase::class.java,
                    DB_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
