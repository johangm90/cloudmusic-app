package com.jgm90.cloudmusic.tables

import android.database.sqlite.SQLiteDatabase

object SongsTable {
    const val TABLE_NAME = "songs"
    const val COL_ID = "id"
    const val COL_NAME = "name"
    const val COL_ARTIST = "artist"
    const val COL_ALBUM = "album"
    const val COL_PIC_ID = "pic_id"
    const val COL_URL_ID = "url_id"
    const val COL_LYRIC_ID = "lyric_id"
    const val COL_SOURCE = "source"
    const val COL_LOCAL_FILE = "local_file"
    const val COL_LOCAL_THUMBNAIL = "local_thumbnail"
    const val COL_LOCAL_LYRIC = "local_lyric"
    const val COL_POSITION = "position"
    const val COL_POSITION_DATE = "position_date"
    const val COL_PLAYLIST_ID = "playlist_id"

    fun create(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME(" +
                "$COL_ID INTEGER UNIQUE," +
                "$COL_NAME TEXT," +
                "$COL_ARTIST TEXT," +
                "$COL_ALBUM TEXT," +
                "$COL_PIC_ID TEXT," +
                "$COL_URL_ID TEXT," +
                "$COL_LYRIC_ID TEXT," +
                "$COL_SOURCE TEXT," +
                "$COL_LOCAL_FILE TEXT," +
                "$COL_LOCAL_THUMBNAIL TEXT," +
                "$COL_LOCAL_LYRIC TEXT," +
                "$COL_POSITION TEXT," +
                "$COL_POSITION_DATE TEXT," +
                "$COL_PLAYLIST_ID INTEGER);"
        )
    }
}
