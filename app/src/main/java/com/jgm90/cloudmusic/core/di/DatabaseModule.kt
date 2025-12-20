package com.jgm90.cloudmusic.core.di

import android.content.Context
import com.jgm90.cloudmusic.core.data.local.CloudMusicDatabase
import com.jgm90.cloudmusic.core.data.local.dao.LikedSongDao
import com.jgm90.cloudmusic.core.data.local.dao.PlaylistDao
import com.jgm90.cloudmusic.core.data.local.dao.RecentSongDao
import com.jgm90.cloudmusic.core.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CloudMusicDatabase {
        return CloudMusicDatabase.getInstance(context)
    }

    @Provides
    fun provideSongDao(database: CloudMusicDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: CloudMusicDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideRecentSongDao(database: CloudMusicDatabase): RecentSongDao = database.recentSongDao()

    @Provides
    fun provideLikedSongDao(database: CloudMusicDatabase): LikedSongDao = database.likedSongDao()
}
