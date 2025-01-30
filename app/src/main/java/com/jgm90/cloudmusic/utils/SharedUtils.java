package com.jgm90.cloudmusic.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.widgets.VulgryMessageView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SharedUtils {

    public static String server = "https://cloudmusicapi.nubit.io/netease/";
    public static int rv_anim_duration = 300;

    public static final String makeShortTimeString(final Context context, long secs) {
        long hours, mins;
        hours = secs / 3600;
        secs %= 3600;
        mins = secs / 60;
        secs %= 60;
        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void set_version(Context context, String version) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("VERSION", version);
        editor.apply();
    }

    public static String get_version(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("VERSION", "0");
    }

    public static void setRepeatMode(Context context, PlaybackMode mode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("MODE", mode.ordinal());
        editor.apply();
    }

    public static PlaybackMode getRepeatMode(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int mode = preferences.getInt("MODE", 0);
        return PlaybackMode.values()[mode];
    }

    public static void setShuffle(Context context, boolean suffle) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("SHUFFLE", suffle);
        editor.apply();
    }

    public static boolean getShuffle(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("SHUFFLE", false);
    }

    public static void showMessage(VulgryMessageView messageView, int pic, int msg) {
        messageView.setDrawable(pic);
        messageView.setText(msg);
        messageView.setVisibility(View.VISIBLE);
    }

    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date date = new Date();
        return dateFormat.format(date);
    }
}
