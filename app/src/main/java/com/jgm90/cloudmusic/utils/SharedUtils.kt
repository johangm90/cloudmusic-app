package com.jgm90.cloudmusic.utils

import android.app.ActivityManager
import android.content.Context
import android.preference.PreferenceManager
import android.view.View
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.widgets.VulgryMessageView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SharedUtils {
    @JvmField
    var server: String = "https://cloudmusicapi.nubit.io/netease/"

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

    fun set_version(context: Context?, version: String?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putString("VERSION", version)
        editor.apply()
    }

    fun get_version(context: Context?): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString("VERSION", "0")!!
    }

    @JvmStatic
    fun setRepeatMode(context: Context?, mode: PlaybackMode) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putInt("MODE", mode.ordinal)
        editor.apply()
    }

    @JvmStatic
    fun getRepeatMode(context: Context?): PlaybackMode {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val mode = preferences.getInt("MODE", 0)
        return PlaybackMode.entries[mode]
    }

    @JvmStatic
    fun setShuffle(context: Context?, suffle: Boolean) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.edit()
        editor.putBoolean("SHUFFLE", suffle)
        editor.apply()
    }

    @JvmStatic
    fun getShuffle(context: Context?): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean("SHUFFLE", false)
    }

    @JvmStatic
    fun showMessage(messageView: VulgryMessageView?, pic: Int, msg: Int) {
        if (messageView == null) return
        messageView.setDrawable(pic)
        messageView.setText(msg)
        messageView.visibility = View.VISIBLE
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
