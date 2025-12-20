package com.jgm90.cloudmusic.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.jgm90.cloudmusic.interfaces.DataTable
import com.jgm90.cloudmusic.models.PlaylistModel
import com.jgm90.cloudmusic.tables.PlaylistsTable
import com.jgm90.cloudmusic.tables.SongsTable
import com.jgm90.cloudmusic.utils.DataUtils

class PlaylistData(context: Context) : BaseData(context), DataTable<PlaylistModel> {
    override fun getOne(filter: String): PlaylistModel? {
        var item: PlaylistModel? = null
        val query = "SELECT * FROM ${PlaylistsTable.TABLE_NAME} WHERE $filter"
        val c: Cursor = db.rawQuery(query, null)
        try {
            if (c.moveToFirst()) {
                item = PlaylistModel(
                    c.getInt(c.getColumnIndex(PlaylistsTable.COL_ID)),
                    c.getString(c.getColumnIndex(PlaylistsTable.COL_NAME)),
                    c.getInt(c.getColumnIndex(PlaylistsTable.COL_OFFLINE)),
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            c.close()
        }
        return item
    }

    override fun getAll(): List<PlaylistModel> {
        val list = mutableListOf<PlaylistModel>()
        val query = "SELECT p.*, COUNT(v.${SongsTable.COL_ID}) AS song_count FROM ${PlaylistsTable.TABLE_NAME} p LEFT JOIN ${SongsTable.TABLE_NAME} v ON v.${SongsTable.COL_PLAYLIST_ID}=p.${PlaylistsTable.COL_ID} GROUP BY p.${PlaylistsTable.COL_ID}"
        val c: Cursor = db.rawQuery(query, null)
        try {
            if (c.moveToFirst()) {
                do {
                    val item = PlaylistModel(
                        c.getInt(c.getColumnIndex(PlaylistsTable.COL_ID)),
                        c.getString(c.getColumnIndex(PlaylistsTable.COL_NAME)),
                        c.getInt(c.getColumnIndex("song_count")),
                        c.getInt(c.getColumnIndex(PlaylistsTable.COL_OFFLINE)),
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

    override fun insert(obj: PlaylistModel) {
        try {
            db.beginTransaction()
            val values: ContentValues = DataUtils<PlaylistModel>().getContentValues(obj)
            values.remove(PlaylistsTable.COL_ID)
            db.insertOrThrow(PlaylistsTable.TABLE_NAME, null, values)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    override fun update(obj: PlaylistModel) {
        try {
            db.beginTransaction()
            val values: ContentValues = DataUtils<PlaylistModel>().getContentValues(obj)
            val rows = db.update(
                PlaylistsTable.TABLE_NAME,
                values,
                "${PlaylistsTable.COL_ID}=?",
                arrayOf(obj.playlist_id.toString()),
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

    override fun delete(obj: PlaylistModel) {
        try {
            db.beginTransaction()
            db.delete(
                SongsTable.TABLE_NAME,
                "${SongsTable.COL_PLAYLIST_ID}=?",
                arrayOf(obj.playlist_id.toString()),
            )
            val rows = db.delete(
                PlaylistsTable.TABLE_NAME,
                "${PlaylistsTable.COL_ID}=?",
                arrayOf(obj.playlist_id.toString()),
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
}
