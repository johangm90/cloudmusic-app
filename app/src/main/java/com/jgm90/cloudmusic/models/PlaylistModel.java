package com.jgm90.cloudmusic.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.jgm90.cloudmusic.interfaces.DataColumn;

public class PlaylistModel implements Parcelable {

    public static final Creator<PlaylistModel> CREATOR = new Creator<PlaylistModel>() {
        public PlaylistModel createFromParcel(Parcel in) {
            return new PlaylistModel(in);
        }

        public PlaylistModel[] newArray(int size) {
            return new PlaylistModel[size];
        }
    };
    @DataColumn
    private int playlist_id;
    @DataColumn
    private String name;
    private int song_count;
    @DataColumn
    private int offline;

    public PlaylistModel(int playlist_id, String name, int offline) {
        this.playlist_id = playlist_id;
        this.name = name;
        this.offline = offline;
    }

    public PlaylistModel(int playlist_id, String name, int song_count, int offline) {
        this.playlist_id = playlist_id;
        this.name = name;
        this.song_count = song_count;
        this.offline = offline;
    }

    public PlaylistModel(Parcel in) {
        playlist_id = in.readInt();
        name = in.readString();
        offline = in.readInt();
    }

    public int getPlaylist_id() {
        return playlist_id;
    }

    public void setPlaylist_id(int playlist_id) {
        this.playlist_id = playlist_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSong_count() {
        return song_count;
    }

    public void setSong_count(int song_count) {
        this.song_count = song_count;
    }

    public int getOffline() {
        return offline;
    }

    public void setOffline(int offline) {
        this.offline = offline;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(playlist_id);
        dest.writeString(name);
        dest.writeInt(offline);
    }
}
