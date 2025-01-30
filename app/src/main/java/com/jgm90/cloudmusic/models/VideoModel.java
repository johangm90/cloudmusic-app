package com.jgm90.cloudmusic.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.jgm90.cloudmusic.interfaces.DataColumn;

public class VideoModel implements Parcelable {

    public static final Parcelable.Creator<VideoModel> CREATOR = new Parcelable.Creator<VideoModel>() {
        public VideoModel createFromParcel(Parcel in) {
            return new VideoModel(in);
        }

        public VideoModel[] newArray(int size) {
            return new VideoModel[size];
        }
    };

    @DataColumn
    private String video_id;
    @DataColumn
    private String title;
    @DataColumn
    private String description;
    @DataColumn
    private String published_at;
    @DataColumn
    private String channel_id;
    @DataColumn
    private String channel_title;
    @DataColumn
    private String thumbnail_small;
    @DataColumn
    private String thumbnail_medium;
    @DataColumn
    private String thumbnail_high;
    @DataColumn
    private String local_file;
    @DataColumn
    private String local_thumbnail;
    @DataColumn
    private int position;
    @DataColumn
    private String position_date;
    @DataColumn
    private int playlist_id;

    public VideoModel(String video_id, String title, String description, String published_at, String channel_id, String channel_title, String thumbnail_small, String thumbnail_medium, String thumbnail_high, String local_file, String local_thumbnail, int position, String position_date, int playlist_id) {
        this.video_id = video_id;
        this.title = title;
        this.description = description;
        this.published_at = published_at;
        this.channel_id = channel_id;
        this.channel_title = channel_title;
        this.thumbnail_small = thumbnail_small;
        this.thumbnail_medium = thumbnail_medium;
        this.thumbnail_high = thumbnail_high;
        this.local_file = local_file;
        this.local_thumbnail = local_thumbnail;
        this.position = position;
        this.position_date = position_date;
        this.playlist_id = playlist_id;
    }

    private VideoModel(Parcel in) {
        video_id = in.readString();
        title = in.readString();
        description = in.readString();
        published_at = in.readString();
        channel_id = in.readString();
        channel_title = in.readString();
        thumbnail_small = in.readString();
        thumbnail_medium = in.readString();
        thumbnail_high = in.readString();
        local_file = in.readString();
        local_thumbnail = in.readString();
        position = in.readInt();
        position_date = in.readString();
        playlist_id = in.readInt();
    }

    public String getVideo_id() {
        return video_id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPublished_at() {
        return published_at;
    }

    public String getChannel_id() {
        return channel_id;
    }

    public String getChannel_title() {
        return channel_title;
    }

    public String getThumbnail_small() {
        return thumbnail_small;
    }

    public String getThumbnail_medium() {
        return thumbnail_medium;
    }

    public String getThumbnail_high() {
        return thumbnail_high;
    }

    public String getLocal_file() {
        return local_file;
    }

    public String getLocal_thumbnail() {
        return local_thumbnail;
    }

    public int getPosition() {
        return position;
    }

    public String getPosition_date() {
        return position_date;
    }

    public int getPlaylist_id() {
        return playlist_id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(video_id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(published_at);
        dest.writeString(channel_id);
        dest.writeString(channel_title);
        dest.writeString(thumbnail_small);
        dest.writeString(thumbnail_medium);
        dest.writeString(thumbnail_high);
        dest.writeString(local_file);
        dest.writeString(local_thumbnail);
        dest.writeInt(position);
        dest.writeString(position_date);
        dest.writeInt(playlist_id);
    }
}
