package com.jgm90.cloudmusic.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.jgm90.cloudmusic.tables.PlaylistsTable;
import com.jgm90.cloudmusic.tables.SongsTable;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "cloudmusic.sqlite";
    private static final int DB_VERSION = 1;
    private static DbHelper instance;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized DbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DbHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        PlaylistsTable.create(db);
        SongsTable.create(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
