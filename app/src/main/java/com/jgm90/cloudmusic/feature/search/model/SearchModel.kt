package com.jgm90.cloudmusic.feature.search.model

import android.os.Parcel
import android.os.Parcelable

class SearchModel(
    val id: String?,
    val title: String?,
    val description: String?,
    val published_at: String?,
    val channel_id: String?,
    val channel_title: String?,
    val thumbnail_small: String?,
    val thumbnail_medium: String?,
    val thumbnail_high: String?,
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        id = parcel.readString(),
        title = parcel.readString(),
        description = parcel.readString(),
        published_at = parcel.readString(),
        channel_id = parcel.readString(),
        channel_title = parcel.readString(),
        thumbnail_small = parcel.readString(),
        thumbnail_medium = parcel.readString(),
        thumbnail_high = parcel.readString(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(published_at)
        dest.writeString(channel_id)
        dest.writeString(channel_title)
        dest.writeString(thumbnail_small)
        dest.writeString(thumbnail_medium)
        dest.writeString(thumbnail_high)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SearchModel> = object : Parcelable.Creator<SearchModel> {
            override fun createFromParcel(parcel: Parcel): SearchModel = SearchModel(parcel)

            override fun newArray(size: Int): Array<SearchModel?> = arrayOfNulls(size)
        }
    }
}
