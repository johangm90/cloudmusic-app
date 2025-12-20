package com.jgm90.cloudmusic.core.model

import android.os.Parcel
import android.os.Parcelable

class LyricModel(
    val songStatus: Int,
    val lyricVersion: Int,
    val lyric: String?,
    val code: Int,
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        songStatus = parcel.readInt(),
        lyricVersion = parcel.readInt(),
        lyric = parcel.readString(),
        code = parcel.readInt(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(songStatus)
        dest.writeInt(lyricVersion)
        dest.writeString(lyric)
        dest.writeInt(code)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<LyricModel> = object : Parcelable.Creator<LyricModel> {
            override fun createFromParcel(parcel: Parcel): LyricModel = LyricModel(parcel)

            override fun newArray(size: Int): Array<LyricModel?> = arrayOfNulls(size)
        }
    }
}
