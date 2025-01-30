package com.jgm90.cloudmusic.events;

public class DownloadEvent {

    public final boolean destination;
    public final int visibility;
    public final String url;
    public final String name;
    public final String filename;

    public DownloadEvent(boolean destination, int visibility, String url, String name, String filename) {
        this.destination = destination;
        this.visibility = visibility;
        this.url = url;
        this.name = name;
        this.filename = filename;
    }
}
