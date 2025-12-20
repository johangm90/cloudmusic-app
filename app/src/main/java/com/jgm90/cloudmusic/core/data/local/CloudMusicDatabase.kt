package com.jgm90.cloudmusic.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jgm90.cloudmusic.core.data.local.dao.PlaylistDao
import com.jgm90.cloudmusic.core.data.local.dao.SongDao
import com.jgm90.cloudmusic.core.data.local.entity.PlaylistEntity
import com.jgm90.cloudmusic.core.data.local.entity.SongEntity

@Database(
    entities = [SongEntity::class, PlaylistEntity::class],
    version = 2,
)
@TypeConverters(Converters::class)
abstract class CloudMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao

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
