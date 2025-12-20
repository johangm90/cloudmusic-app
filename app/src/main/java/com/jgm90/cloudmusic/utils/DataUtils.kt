package com.jgm90.cloudmusic.utils

import android.content.ContentValues
import android.text.TextUtils
import android.util.Log
import com.jgm90.cloudmusic.interfaces.DataColumn

class DataUtils<T : Any> {
    fun getContentValues(obj: T): ContentValues {
        val map = HashMap<String, Any?>()
        for (field in obj.javaClass.declaredFields) {
            field.isAccessible = true
            try {
                if (field.isAnnotationPresent(DataColumn::class.java)) {
                    map[field.name] = field.get(obj)
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        val contentValues = ContentValues()
        for ((key, value) in map) {
            when (value) {
                is Int -> contentValues.put(key, value)
                is String -> contentValues.put(key, value)
                is List<*> -> {
                    val values = TextUtils.join(",", value)
                    contentValues.put(key, values)
                }
                else -> Log.w("DataUtils", "Key: $key Value: ${value?.toString() ?: "null"}")
            }
        }
        return contentValues
    }
}
