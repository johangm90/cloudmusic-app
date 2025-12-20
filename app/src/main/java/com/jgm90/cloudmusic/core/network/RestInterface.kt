package com.jgm90.cloudmusic.core.network

import com.jgm90.cloudmusic.core.model.LyricModel
import com.jgm90.cloudmusic.core.model.SongModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RestInterface {
    @GET("search/{query}/{page}/{limit}")
    suspend fun getSongs(
        @Path("query") query: String?,
        @Path("page") page: Int,
        @Path("limit") limit: Int
    ): List<SongModel>

    @GET("lyric/{id}")
    fun getLyrics(@Path("id") id: String?): Call<LyricModel?>
}
