package com.jgm90.cloudmusic.tables

import android.database.sqlite.SQLiteDatabase

object PlaylistsTable {
    const val TABLE_NAME = "playlists"
    const val COL_ID = "playlist_id"
    const val COL_NAME = "name"
    const val COL_OFFLINE = "offline"

    fun create(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME(" +
                "$COL_ID INTEGER PRIMARY KEY," +
                "$COL_NAME TEXT UNIQUE," +
                "$COL_OFFLINE NUMERIC);"
        )
    }
}
