package com.jgm90.cloudmusic.events;

public class IsPlayingEvent {

    public final boolean isPlaying;

    public IsPlayingEvent(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }
}
