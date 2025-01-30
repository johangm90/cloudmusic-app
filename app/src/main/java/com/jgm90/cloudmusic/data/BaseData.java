package com.jgm90.cloudmusic.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.jgm90.cloudmusic.utils.DbHelper;

public class BaseData {

    public DbHelper helper;
    public SQLiteDatabase db;
    public Context context;

    public BaseData(Context context) {
        this.context = context;
        helper = DbHelper.getInstance(context);
        db = helper.getWritableDatabase();
    }
}
