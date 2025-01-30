package com.jgm90.cloudmusic.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.jgm90.cloudmusic.interfaces.DataTable;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.tables.SongsTable;
import com.jgm90.cloudmusic.utils.DataUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SongData extends BaseData implements DataTable<SongModel> {

    public SongData(Context context) {
        super(context);
    }

    @Override
    public SongModel getOne(String filter) {
        SongModel item = null;
        String query = "SELECT * FROM " + SongsTable.TABLE_NAME + " WHERE " + filter;
        Cursor c = db.rawQuery(query, null);
        try {
            if (c.moveToFirst()) {
                List<String> artists = Arrays.asList(c.getString(c.getColumnIndex(SongsTable.COL_ARTIST)).split(","));
                item = new SongModel(
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
                        c.getInt(c.getColumnIndex(SongsTable.COL_PLAYLIST_ID))
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
    public List<SongModel> getAll() {
        List<SongModel> list = new ArrayList<>();
        String query = "SELECT * FROM " + SongsTable.TABLE_NAME;
        Cursor c = db.rawQuery(query, null);
        try {
            if (c.moveToFirst()) {
                do {
                    List<String> artists = Arrays.asList(c.getString(c.getColumnIndex(SongsTable.COL_ARTIST)).split(","));
                    SongModel item = new SongModel(
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
                            c.getInt(c.getColumnIndex(SongsTable.COL_PLAYLIST_ID))
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

    public List<SongModel> getAllFilter(String filter) {
        List<SongModel> list = new ArrayList<>();
        String query = "SELECT * FROM " + SongsTable.TABLE_NAME + " WHERE " + filter + " ORDER BY position, position_date";
        Cursor c = db.rawQuery(query, null);
        try {
            if (c.moveToFirst()) {
                do {
                    List<String> artists = Arrays.asList(c.getString(c.getColumnIndex(SongsTable.COL_ARTIST)).split(","));
                    SongModel item = new SongModel(
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
                            c.getInt(c.getColumnIndex(SongsTable.COL_PLAYLIST_ID))
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
    public void insert(SongModel obj) {
        try {
            db.beginTransaction();
            ContentValues values = new DataUtils<SongModel>().getContentValues(obj);
            db.insertOrThrow(SongsTable.TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void update(SongModel obj) {
        try {
            db.beginTransaction();
            ContentValues values = new DataUtils<SongModel>().getContentValues(obj);
            int rows = db.update(SongsTable.TABLE_NAME, values, SongsTable.COL_ID + "=?",
                    new String[]{obj.getId()});
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
    public void delete(SongModel obj) {
        try {
            db.beginTransaction();
            int rows = db.delete(SongsTable.TABLE_NAME, SongsTable.COL_ID + "=?",
                    new String[]{obj.getId()});
            if (rows == 1) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public int getNextPosition() {
        int pos = 0;
        String query = "SELECT MAX(position) AS position FROM " + SongsTable.TABLE_NAME;
        Cursor c = db.rawQuery(query, null);
        try {
            if (c.moveToFirst()) {
                pos = c.getInt(c.getColumnIndex(SongsTable.COL_POSITION));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return pos + 1;
    }
}
