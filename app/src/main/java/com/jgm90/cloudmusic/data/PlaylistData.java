package com.jgm90.cloudmusic.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.jgm90.cloudmusic.interfaces.DataTable;
import com.jgm90.cloudmusic.models.PlaylistModel;
import com.jgm90.cloudmusic.tables.PlaylistsTable;
import com.jgm90.cloudmusic.tables.SongsTable;
import com.jgm90.cloudmusic.utils.DataUtils;

import java.util.ArrayList;
import java.util.List;

public class PlaylistData extends BaseData implements DataTable<PlaylistModel> {

    public PlaylistData(Context context) {
        super(context);
    }

    @Override
    public PlaylistModel getOne(String filter) {
        PlaylistModel item = null;
        String query = "SELECT * FROM " + PlaylistsTable.TABLE_NAME + " WHERE " + filter;
        Cursor c = db.rawQuery(query, null);
        try {
            if (c.moveToFirst()) {
                item = new PlaylistModel(
                        c.getInt(c.getColumnIndex(PlaylistsTable.COL_ID)),
                        c.getString(c.getColumnIndex(PlaylistsTable.COL_NAME)),
                        c.getInt(c.getColumnIndex(PlaylistsTable.COL_OFFLINE))
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return item;
    }

    @Override
    public List<PlaylistModel> getAll() {
        List<PlaylistModel> list = new ArrayList<>();
        String query = "SELECT p.*, COUNT(v." + SongsTable.COL_ID + ") AS song_count FROM " + PlaylistsTable.TABLE_NAME + " p LEFT JOIN " + SongsTable.TABLE_NAME + " v ON v." + SongsTable.COL_PLAYLIST_ID + "=" + "p." + PlaylistsTable.COL_ID + " GROUP BY p." + PlaylistsTable.COL_ID;
        Cursor c = db.rawQuery(query, null);
        try {
            if (c.moveToFirst()) {
                do {
                    PlaylistModel item = new PlaylistModel(
                            c.getInt(c.getColumnIndex(PlaylistsTable.COL_ID)),
                            c.getString(c.getColumnIndex(PlaylistsTable.COL_NAME)),
                            c.getInt(c.getColumnIndex("song_count")),
                            c.getInt(c.getColumnIndex(PlaylistsTable.COL_OFFLINE))
                    );
                    list.add(item);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return list;
    }

    @Override
    public void insert(PlaylistModel obj) {
        try {
            db.beginTransaction();
            ContentValues values = new DataUtils<PlaylistModel>().getContentValues(obj);
            values.remove(PlaylistsTable.COL_ID);
            db.insertOrThrow(PlaylistsTable.TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void update(PlaylistModel obj) {
        try {
            db.beginTransaction();
            ContentValues values = new DataUtils<PlaylistModel>().getContentValues(obj);
            int rows = db.update(PlaylistsTable.TABLE_NAME, values, PlaylistsTable.COL_ID + "=?",
                    new String[]{String.valueOf(obj.getPlaylist_id())});
            if (rows == 1) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void delete(PlaylistModel obj) {
        try {
            db.beginTransaction();
            db.delete(SongsTable.TABLE_NAME, SongsTable.COL_PLAYLIST_ID + "=?",
                    new String[]{String.valueOf(obj.getPlaylist_id())});
            int rows = db.delete(PlaylistsTable.TABLE_NAME, PlaylistsTable.COL_ID + "=?",
                    new String[]{String.valueOf(obj.getPlaylist_id())});
            if (rows == 1) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }
}
