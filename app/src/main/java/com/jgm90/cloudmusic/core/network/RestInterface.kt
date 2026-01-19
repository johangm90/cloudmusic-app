package com.jgm90.cloudmusic.core.network

import com.jgm90.cloudmusic.core.model.LyricModel
import com.jgm90.cloudmusic.core.model.SongModel
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RestInterface {
    @GET("search")
    suspend fun getSongs(
        @Query("q") query: String?
    ): List<SongModel>

    @GET("lyric")
    suspend fun getLyrics(@Query("id") id: String?): LyricModel?
}
