package com.jgm90.cloudmusic.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jgm90.cloudmusic.utils.DbHelper

open class BaseData(val context: Context) {
    val helper: DbHelper = DbHelper.getInstance(context)
    val db: SQLiteDatabase = helper.writableDatabase
}
