package com.jgm90.cloudmusic.utils

import java.util.Arrays
import java.util.Collections
import java.util.regex.Pattern

class Lyrics {
    companion object {
        private const val LRC_LINE_REGEXP = "\\[([0-9\\.:]+)](.*)"
        private const val LRC_TIME_LONG_REGEXP = "^([0-9]{2}):([0-9]{2})\\.([0-9]{2})$"
        private const val LRC_TIME_SHORT_REGEXP = "^([0-9]{2}):([0-9]{2})$"
        private var lyrics: MutableList<LyricLine> = emptyList<LyricLine>().toMutableList()
        private var offset = 0.0

        @JvmStatic
        fun parse(lrc: String): List<LyricLine>? {
            var lrc = lrc
            try {
                //separate lines
                lrc = lrc.replace("\n", "").replace("\r", "")
                var split =
                    lrc.replace("[", "\n[").split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                split = Arrays.copyOfRange(split, 1, split.size - 1)


                //get offset tag if present
                val offsetPattern = Pattern.compile("\\[offset: ([-0-9]+)].*")

                //get lines with valid tag
                lyrics = ArrayList()

                val linePattern = Pattern.compile(LRC_LINE_REGEXP)
                for (line in split) {
                    val lineMatcher = linePattern.matcher(line)

                    if (lineMatcher.matches()) {
                        lyrics.add(
                            LyricLine(
                                getSecondsFromTag(lineMatcher.group(1)),
                                lineMatcher.group(2)
                            )
                        )
                    }

                    val offsetMatcher = offsetPattern.matcher(line)
                    if (offsetMatcher.matches()) {
                        //get offset (ms)
                        offset = offsetMatcher.group(1).toDouble() / 1000.0
                    }
                }
                Collections.sort(lyrics, LyricLine.COMPARATOR)
                return lyrics
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        private fun getSecondsFromTag(tag: String): Double {
            var seconds = 0.0
            if (tag.length == 9) {
                //00:00.000
                seconds += (tag.substring(0, 2).toInt() * 60).toDouble()
                seconds += tag.substring(3, 5).toInt().toDouble()
                seconds += tag.substring(6, 8).toInt() / 100.0
                return seconds
            } else if (tag.length == 8) {
                //00:00.00
                seconds += (tag.substring(0, 2).toInt() * 60).toDouble()
                seconds += tag.substring(3, 5).toInt().toDouble()
                seconds += tag.substring(6, 8).toInt() / 100.0
                return seconds
            } else if (tag.length == 5) {
                //00:00
                seconds += (tag.substring(0, 2).toInt() * 60).toDouble()
                seconds += tag.substring(3, 5).toInt().toDouble()
                return seconds
            } else throw IllegalArgumentException("Not a valid time tag.")
        }
    }
}