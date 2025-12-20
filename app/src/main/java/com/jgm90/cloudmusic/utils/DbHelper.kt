package com.jgm90.cloudmusic.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jgm90.cloudmusic.tables.PlaylistsTable
import com.jgm90.cloudmusic.tables.SongsTable

class DbHelper private constructor(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        PlaylistsTable.create(db)
        SongsTable.create(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    companion object {
        private const val DB_NAME = "cloudmusic.sqlite"
        private const val DB_VERSION = 1
        @Volatile
        private var instance: DbHelper? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): DbHelper {
            if (instance == null) {
                instance = DbHelper(context.applicationContext)
            }
            return requireNotNull(instance)
        }
    }
}
