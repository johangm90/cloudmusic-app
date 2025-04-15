package com.jgm90.cloudmusic.interfaces

import com.jgm90.cloudmusic.models.LyricModel
import com.jgm90.cloudmusic.models.SongModel
import com.jgm90.cloudmusic.models.UpdateModel
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

    @GET("song/{id}")
    fun getSongDetail(@Path("id") id: String?): Call<List<SongModel>>

    @GET("lyric/{id}")
    fun getLyrics(@Path("id") id: String?): Call<LyricModel?>

    @GET("update")
    fun checkUpdate(): Call<UpdateModel>
}
