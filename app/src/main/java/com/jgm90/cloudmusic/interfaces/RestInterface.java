package com.jgm90.cloudmusic.interfaces;

import com.jgm90.cloudmusic.models.LyricModel;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.models.UpdateModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RestInterface {

    @GET("search/{query}/{page}/{limit}")
    Call<List<SongModel>> getSongs(@Path("query") String query, @Path("page") int page, @Path("limit") int limit);

    @GET("song/{id}")
    Call<List<SongModel>> getSongDetail(@Path("id") String id);

    @GET("lyric/{id}")
    Call<LyricModel> getLyrics(@Path("id") String id);

    @GET("update")
    Call<UpdateModel> checkUpdate();
}
