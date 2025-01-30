package com.jgm90.cloudmusic.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.jgm90.cloudmusic.interfaces.DataColumn;

import java.util.ArrayList;
import java.util.List;

public class SongModel implements Parcelable {

    public static final Creator<SongModel> CREATOR = new Creator<SongModel>() {
        public SongModel createFromParcel(Parcel in) {
            return new SongModel(in);
        }

        public SongModel[] newArray(int size) {
            return new SongModel[size];
        }
    };
    @DataColumn
    private String id;
    @DataColumn
    private String name;
    @DataColumn
    private List<String> artist = null;
    @DataColumn
    private String album;
    @DataColumn
    private String pic_id;
    @DataColumn
    private String url_id;
    @DataColumn
    private String lyric_id;
    @DataColumn
    private String source;
    @DataColumn
    private String local_file;
    @DataColumn
    private String local_thumbnail;
    @DataColumn
    private String local_lyric;
    @DataColumn
    private int position;
    @DataColumn
    private String position_date;
    @DataColumn
    private int playlist_id;

    public SongModel(String id, String name, List<String> artist, String album, String pic_id, String url_id, String lyric_id, String source) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.pic_id = pic_id;
        this.url_id = url_id;
        this.lyric_id = lyric_id;
        this.source = source;
    }

    public SongModel(String id, String name, List<String> artist, String album, String pic_id, String url_id, String lyric_id, String source, String local_file, String local_thumbnail, String local_lyric, int position, String position_date, int playlist_id) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.pic_id = pic_id;
        this.url_id = url_id;
        this.lyric_id = lyric_id;
        this.source = source;
        this.local_file = local_file;
        this.local_thumbnail = local_thumbnail;
        this.local_lyric = local_lyric;
        this.position = position;
        this.position_date = position_date;
        this.playlist_id = playlist_id;
    }

    public SongModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        artist = new ArrayList<>();
        in.readList(artist, null);
        album = in.readString();
        pic_id = in.readString();
        url_id = in.readString();
        lyric_id = in.readString();
        source = in.readString();
        local_file = in.readString();
        local_thumbnail = in.readString();
        local_lyric = in.readString();
        position = in.readInt();
        position_date = in.readString();
        playlist_id = in.readInt();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArtist() {
        return artist;
    }

    public void setArtist(List<String> artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getPic_id() {
        return pic_id;
    }

    public void setPic_id(String pic_id) {
        this.pic_id = pic_id;
    }

    public String getUrl_id() {
        return url_id;
    }

    public void setUrl_id(String url_id) {
        this.url_id = url_id;
    }

    public String getLyric_id() {
        return lyric_id;
    }

    public void setLyric_id(String lyric_id) {
        this.lyric_id = lyric_id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocal_file() {
        return local_file;
    }

    public void setLocal_file(String local_file) {
        this.local_file = local_file;
    }

    public String getLocal_thumbnail() {
        return local_thumbnail;
    }

    public void setLocal_thumbnail(String local_thumbnail) {
        this.local_thumbnail = local_thumbnail;
    }

    public String getLocal_lyric() {
        return local_lyric;
    }

    public void setLocal_lyric(String local_lyric) {
        this.local_lyric = local_lyric;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getPosition_date() {
        return position_date;
    }

    public void setPosition_date(String position_date) {
        this.position_date = position_date;
    }

    public int getPlaylist_id() {
        return playlist_id;
    }

    public void setPlaylist_id(int playlist_id) {
        this.playlist_id = playlist_id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeList(artist);
        dest.writeString(album);
        dest.writeString(pic_id);
        dest.writeString(url_id);
        dest.writeString(lyric_id);
        dest.writeString(source);
        dest.writeString(local_file);
        dest.writeString(local_thumbnail);
        dest.writeString(local_lyric);
        dest.writeInt(position);
        dest.writeString(position_date);
        dest.writeInt(playlist_id);
    }
}
