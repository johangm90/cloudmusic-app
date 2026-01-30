package com.jgm90.cloudmusic.core.util

import android.app.ActivityManager
import android.content.Context
import com.jgm90.cloudmusic.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SharedUtils {
    @JvmField
    var server: String = "https://musicapi.nubit.io/"
    @JvmField
    var rv_anim_duration: Int = 300

    @JvmStatic
    fun makeShortTimeString(context: Context, secs: Long): String {
        var secs = secs
        val hours = secs / 3600
        secs %= 3600
        val mins = secs / 60
        secs %= 60
        val durationFormat = context.resources.getString(
            if (hours == 0L) R.string.durationformatshort else R.string.durationformatlong
        )
        return String.format(durationFormat, hours, mins, secs)
    }

    @JvmStatic
    fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    val dateTime: String
        get() {
            val dateFormat: DateFormat =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = Date()
            return dateFormat.format(date)
        }
}
