package com.jgm90.cloudmusic.core.playback

import android.util.Log

class LyricLine internal constructor(private var time: Double, @JvmField var lyric: String) {
    override fun toString(): String {
        return "LyricLine{" +
                time +
                ", '" + lyric + '\'' +
                '}'
    }

    fun getTime(): Double {
        return time
    }

    fun setTime(time: Double) {
        this.time = time
        Log.d("LyricsPlayer.LyricsLine", "Time set to = $time")
    }

    val timeTag: String
        get() {
            var t = (getTime() * 100).toInt()
            val minutes = (t / 6000)
            t -= minutes * 6000
            val seconds = t / 100
            t -= seconds * 100
            return String.format("[%02d:%02d.%02d]", minutes, seconds, t)
        }

    companion object {
        val COMPARATOR: Comparator<LyricLine> =
            Comparator { lyricLine, lyricLine2 -> (lyricLine.getTime() - lyricLine2.getTime()).toInt() }
    }
}
