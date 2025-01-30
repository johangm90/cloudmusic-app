package com.jgm90.cloudmusic.tables;

import android.database.sqlite.SQLiteDatabase;

public class PlaylistsTable {

    public static String TABLE_NAME = "playlists";
    public static String COL_ID = "playlist_id";
    public static String COL_NAME = "name";
    public static String COL_OFFLINE = "offline";

    public static void create(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(" +
                COL_ID + " INTEGER PRIMARY KEY," +
                COL_NAME + " TEXT UNIQUE," +
                COL_OFFLINE + " NUMERIC);");
    }
}
