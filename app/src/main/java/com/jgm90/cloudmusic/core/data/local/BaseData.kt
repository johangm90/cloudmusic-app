package com.jgm90.cloudmusic.core.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jgm90.cloudmusic.core.data.local.DbHelper

open class BaseData(val context: Context) {
    val helper: DbHelper = DbHelper.getInstance(context)
    val db: SQLiteDatabase = helper.writableDatabase
}
