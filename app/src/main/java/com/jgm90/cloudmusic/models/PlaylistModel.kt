package com.jgm90.cloudmusic.models

import android.os.Parcel
import android.os.Parcelable
import com.jgm90.cloudmusic.interfaces.DataColumn

class PlaylistModel(
    @field:DataColumn var playlist_id: Int,
    @field:DataColumn var name: String,
    var song_count: Int,
    @field:DataColumn var offline: Int,
) : Parcelable {
    constructor(playlist_id: Int, name: String, offline: Int) : this(
        playlist_id = playlist_id,
        name = name,
        song_count = 0,
        offline = offline,
    )

    private constructor(parcel: Parcel) : this(
        playlist_id = parcel.readInt(),
        name = parcel.readString() ?: "",
        song_count = 0,
        offline = parcel.readInt(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(playlist_id)
        dest.writeString(name)
        dest.writeInt(offline)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PlaylistModel> = object : Parcelable.Creator<PlaylistModel> {
            override fun createFromParcel(parcel: Parcel): PlaylistModel = PlaylistModel(parcel)

            override fun newArray(size: Int): Array<PlaylistModel?> = arrayOfNulls(size)
        }
    }
}
