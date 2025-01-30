package com.jgm90.cloudmusic.utils;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

import com.jgm90.cloudmusic.interfaces.DataColumn;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataUtils<T> {

    public ContentValues getContentValues(T obj) {
        HashMap<String, Object> map = new HashMap<>();
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                if (field.isAnnotationPresent(DataColumn.class)) {
                    map.put(field.getName(), field.get(obj));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        ContentValues contentValues = new ContentValues();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            if (entry.getValue() instanceof Integer) {
                contentValues.put(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof String) {
                contentValues.put(entry.getKey(), (entry.getValue() == null) ? "" : (String) entry.getValue());
            } else if (entry.getValue() instanceof List) {
                String values = TextUtils.join(",", (List) entry.getValue());
                contentValues.put(entry.getKey(), (entry.getKey() == null) ? "" : values);
            } else {
                Log.w("DataUtils", "Key: " + entry.getKey() + " Value: " + String.valueOf(entry.getKey()));
            }
        }
        return contentValues;
    }
}
