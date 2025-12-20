package com.jgm90.cloudmusic.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.jgm90.cloudmusic.interfaces.DataTable
import com.jgm90.cloudmusic.models.SongModel
import com.jgm90.cloudmusic.tables.SongsTable
import com.jgm90.cloudmusic.utils.DataUtils

class SongData(context: Context) : BaseData(context), DataTable<SongModel> {
    override fun getOne(filter: String): SongModel? {
        var item: SongModel? = null
        val query = "SELECT * FROM ${SongsTable.TABLE_NAME} WHERE $filter"
        val c: Cursor = db.rawQuery(query, null)
        try {
            if (c.moveToFirst()) {
                val artists = c.getString(c.getColumnIndex(SongsTable.COL_ARTIST)).split(",")
                item = SongModel(
                    c.getString(c.getColumnIndex(SongsTable.COL_ID)),
                    c.getString(c.getColumnIndex(SongsTable.COL_NAME)),
                    artists,
                    c.getString(c.getColumnIndex(SongsTable.COL_ALBUM)),
                    c.getString(c.getColumnIndex(SongsTable.COL_PIC_ID)),
                    c.getString(c.getColumnIndex(SongsTable.COL_URL_ID)),
                    c.getString(c.getColumnIndex(SongsTable.COL_LYRIC_ID)),
                    c.getString(c.getColumnIndex(SongsTable.COL_SOURCE)),
                    c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_FILE)),
                    c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_THUMBNAIL)),
                    c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_LYRIC)),
                    c.getInt(c.getColumnIndex(SongsTable.COL_POSITION)),
                    c.getString(c.getColumnIndex(SongsTable.COL_POSITION_DATE)),
                    c.getInt(c.getColumnIndex(SongsTable.COL_PLAYLIST_ID)),
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            c.close()
        }
        return item
    }

    override fun getAll(): List<SongModel> {
        val list = mutableListOf<SongModel>()
        val query = "SELECT * FROM ${SongsTable.TABLE_NAME}"
        val c: Cursor = db.rawQuery(query, null)
        try {
            if (c.moveToFirst()) {
                do {
                    val artists = c.getString(c.getColumnIndex(SongsTable.COL_ARTIST)).split(",")
                    val item = SongModel(
                        c.getString(c.getColumnIndex(SongsTable.COL_ID)),
                        c.getString(c.getColumnIndex(SongsTable.COL_NAME)),
                        artists,
                        c.getString(c.getColumnIndex(SongsTable.COL_ALBUM)),
                        c.getString(c.getColumnIndex(SongsTable.COL_PIC_ID)),
                        c.getString(c.getColumnIndex(SongsTable.COL_URL_ID)),
                        c.getString(c.getColumnIndex(SongsTable.COL_LYRIC_ID)),
                        c.getString(c.getColumnIndex(SongsTable.COL_SOURCE)),
                        c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_FILE)),
                        c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_THUMBNAIL)),
                        c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_LYRIC)),
                        c.getInt(c.getColumnIndex(SongsTable.COL_POSITION)),
                        c.getString(c.getColumnIndex(SongsTable.COL_POSITION_DATE)),
                        c.getInt(c.getColumnIndex(SongsTable.COL_PLAYLIST_ID)),
                    )
                    list.add(item)
                } while (c.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            c.close()
        }
        return list
    }

    fun getAllFilter(filter: String): List<SongModel> {
        val list = mutableListOf<SongModel>()
        val query = "SELECT * FROM ${SongsTable.TABLE_NAME} WHERE $filter ORDER BY position, position_date"
        val c: Cursor = db.rawQuery(query, null)
        try {
            if (c.moveToFirst()) {
                do {
                    val artists = c.getString(c.getColumnIndex(SongsTable.COL_ARTIST)).split(",")
                    val item = SongModel(
                        c.getString(c.getColumnIndex(SongsTable.COL_ID)),
                        c.getString(c.getColumnIndex(SongsTable.COL_NAME)),
                        artists,
                        c.getString(c.getColumnIndex(SongsTable.COL_ALBUM)),
                        c.getString(c.getColumnIndex(SongsTable.COL_PIC_ID)),
                        c.getString(c.getColumnIndex(SongsTable.COL_URL_ID)),
                        c.getString(c.getColumnIndex(SongsTable.COL_LYRIC_ID)),
                        c.getString(c.getColumnIndex(SongsTable.COL_SOURCE)),
                        c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_FILE)),
                        c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_THUMBNAIL)),
                        c.getString(c.getColumnIndex(SongsTable.COL_LOCAL_LYRIC)),
                        c.getInt(c.getColumnIndex(SongsTable.COL_POSITION)),
                        c.getString(c.getColumnIndex(SongsTable.COL_POSITION_DATE)),
                        c.getInt(c.getColumnIndex(SongsTable.COL_PLAYLIST_ID)),
                    )
                    list.add(item)
                } while (c.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            c.close()
        }
        return list
    }

    override fun insert(obj: SongModel) {
        try {
            db.beginTransaction()
            val values: ContentValues = DataUtils<SongModel>().getContentValues(obj)
            db.insertOrThrow(SongsTable.TABLE_NAME, null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    override fun update(obj: SongModel) {
        try {
            db.beginTransaction()
            val values: ContentValues = DataUtils<SongModel>().getContentValues(obj)
            val rows = db.update(
                SongsTable.TABLE_NAME,
                values,
                "${SongsTable.COL_ID}=?",
                arrayOf(obj.id),
            )
            if (rows == 1) {
                db.setTransactionSuccessful()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    override fun delete(obj: SongModel) {
        try {
            db.beginTransaction()
            val rows = db.delete(
                SongsTable.TABLE_NAME,
                "${SongsTable.COL_ID}=?",
                arrayOf(obj.id),
            )
            if (rows == 1) {
                db.setTransactionSuccessful()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun getNextPosition(): Int {
        var pos = 0
        val query = "SELECT MAX(position) AS position FROM ${SongsTable.TABLE_NAME}"
        val c: Cursor = db.rawQuery(query, null)
        try {
            if (c.moveToFirst()) {
                pos = c.getInt(c.getColumnIndex(SongsTable.COL_POSITION))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            c.close()
        }
        return pos + 1
    }
}
