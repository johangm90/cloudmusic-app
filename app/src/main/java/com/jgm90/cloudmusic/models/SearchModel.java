package com.jgm90.cloudmusic.models;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchModel implements Parcelable {

    public static final Parcelable.Creator<SearchModel> CREATOR = new Parcelable.Creator<SearchModel>() {
        public SearchModel createFromParcel(Parcel in) {
            return new SearchModel(in);
        }

        public SearchModel[] newArray(int size) {
            return new SearchModel[size];
        }
    };
    private String id;
    private String title;
    private String description;
    private String published_at;
    private String channel_id;
    private String channel_title;
    private String thumbnail_small;
    private String thumbnail_medium;
    private String thumbnail_high;

    public SearchModel(String id, String title, String description, String published_at, String channel_id, String channel_title, String thumbnail_small, String thumbnail_medium, String thumbnail_high) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.published_at = published_at;
        this.channel_id = channel_id;
        this.channel_title = channel_title;
        this.thumbnail_small = thumbnail_small;
        this.thumbnail_medium = thumbnail_medium;
        this.thumbnail_high = thumbnail_high;
    }

    private SearchModel(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        published_at = in.readString();
        channel_id = in.readString();
        channel_title = in.readString();
        thumbnail_small = in.readString();
        thumbnail_medium = in.readString();
        thumbnail_high = in.readString();
    }

    public String getId() {
        return id;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(published_at);
        dest.writeString(channel_id);
        dest.writeString(channel_title);
        dest.writeString(thumbnail_small);
        dest.writeString(thumbnail_medium);
        dest.writeString(thumbnail_high);
    }
}
