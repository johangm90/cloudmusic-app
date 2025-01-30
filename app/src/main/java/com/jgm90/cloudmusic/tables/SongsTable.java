package com.jgm90.cloudmusic.tables;

import android.database.sqlite.SQLiteDatabase;

public class SongsTable {

    public static String TABLE_NAME = "songs";
    public static String COL_ID = "id";
    public static String COL_NAME = "name";
    public static String COL_ARTIST = "artist";
    public static String COL_ALBUM = "album";
    public static String COL_PIC_ID = "pic_id";
    public static String COL_URL_ID = "url_id";
    public static String COL_LYRIC_ID = "lyric_id";
    public static String COL_SOURCE = "source";
    public static String COL_LOCAL_FILE = "local_file";
    public static String COL_LOCAL_THUMBNAIL = "local_thumbnail";
    public static String COL_LOCAL_LYRIC = "local_lyric";
    public static String COL_POSITION = "position";
    public static String COL_POSITION_DATE = "position_date";
    public static String COL_PLAYLIST_ID = "playlist_id";

    public static void create(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(" +
                COL_ID + " INTEGER UNIQUE," +
                COL_NAME + " TEXT," +
                COL_ARTIST + " TEXT," +
                COL_ALBUM + " TEXT," +
                COL_PIC_ID + " TEXT," +
                COL_URL_ID + " TEXT," +
                COL_LYRIC_ID + " TEXT," +
                COL_SOURCE + " TEXT," +
                COL_LOCAL_FILE + " TEXT," +
                COL_LOCAL_THUMBNAIL + " TEXT," +
                COL_LOCAL_LYRIC + " TEXT," +
                COL_POSITION + " TEXT," +
                COL_POSITION_DATE + " TEXT," +
                COL_PLAYLIST_ID + " INTEGER);");
    }
}
