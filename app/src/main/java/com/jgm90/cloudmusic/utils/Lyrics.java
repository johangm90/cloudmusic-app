package com.jgm90.cloudmusic.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lyrics {

    private static final String LRC_LINE_REGEXP = "\\[([0-9\\.:]+)](.*)";
    private static final String LRC_TIME_LONG_REGEXP = "^([0-9]{2}):([0-9]{2})\\.([0-9]{2})$";
    private static final String LRC_TIME_SHORT_REGEXP = "^([0-9]{2}):([0-9]{2})$";
    private static List<LyricLine> lyrics;
    private static double offset;

    public static List<LyricLine> parse(String lrc) {
        try {
            //separate lines
            lrc = lrc.replace("\n", "").replace("\r", "");
            String[] split = lrc.replace("[", "\n[").split("\n");
            split = Arrays.copyOfRange(split, 1, split.length - 1);


            //get offset tag if present
            Pattern offsetPattern = Pattern.compile("\\[offset: ([-0-9]+)].*");

            //get lines with valid tag
            lyrics = new ArrayList<LyricLine>();

            Pattern linePattern = Pattern.compile(LRC_LINE_REGEXP);
            for (String line : split) {
                Matcher lineMatcher = linePattern.matcher(line);

                if (lineMatcher.matches()) {
                    lyrics.add(new LyricLine(getSecondsFromTag(lineMatcher.group(1)), lineMatcher.group(2)));
                }

                Matcher offsetMatcher = offsetPattern.matcher(line);
                if (offsetMatcher.matches()) {
                    //get offset (ms)
                    offset = Double.parseDouble(offsetMatcher.group(1)) / 1000d;
                }
            }
            Collections.sort(lyrics, LyricLine.COMPARATOR);
            return lyrics;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static double getSecondsFromTag(String tag) {
        double seconds = 0;
        if (tag.length() == 9) {
            //00:00.000
            seconds += Integer.parseInt(tag.substring(0, 2)) * 60;
            seconds += Integer.parseInt(tag.substring(3, 5));
            seconds += Integer.parseInt(tag.substring(6, 8)) / 100d;
            return seconds;
        } else if (tag.length() == 8) {
            //00:00.00
            seconds += Integer.parseInt(tag.substring(0, 2)) * 60;
            seconds += Integer.parseInt(tag.substring(3, 5));
            seconds += Integer.parseInt(tag.substring(6, 8)) / 100d;
            return seconds;
        } else if (tag.length() == 5) {
            //00:00
            seconds += Integer.parseInt(tag.substring(0, 2)) * 60;
            seconds += Integer.parseInt(tag.substring(3, 5));
            return seconds;
        } else throw new IllegalArgumentException("Not a valid time tag.");

    }

    public List<LyricLine> getAllLyrics() {
        return lyrics;
    }
}