package com.jgm90.cloudmusic.models;

import android.os.Parcel;
import android.os.Parcelable;

public class LyricModel implements Parcelable {
    private final int songStatus;
    private final int lyricVersion;
    private String lyric;
    private final int code;

    public LyricModel(int songStatus, int lyricVersion, String lyric, int code) {
        this.songStatus = songStatus;
        this.lyricVersion = lyricVersion;
        this.lyric = lyric;
        this.code = code;
    }

    public int getSongStatus() {
        return songStatus;
    }

    public int getLyricVersion() {
        return lyricVersion;
    }

    public String getLyric() {
        return lyric;
    }

    public int getCode() {
        return code;
    }

    public LyricModel(Parcel in) {
        songStatus = in.readInt();
        lyricVersion = in.readInt();
        lyric = in.readString();
        code = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(songStatus);
        dest.writeInt(lyricVersion);
        dest.writeString(lyric);
        dest.writeInt(code);
    }

    public static final Parcelable.Creator<LyricModel> CREATOR = new Parcelable.Creator<LyricModel>() {
        public LyricModel createFromParcel(Parcel in) {
            return new LyricModel(in);
        }

        public LyricModel[] newArray(int size) {
            return new LyricModel[size];
        }
    };
}
