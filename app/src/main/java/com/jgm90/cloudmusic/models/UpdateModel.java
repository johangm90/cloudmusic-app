package com.jgm90.cloudmusic.models;

public class UpdateModel {

    private String app;
    private String version;
    private String published_at;
    private String download_url;

    public UpdateModel(String app, String version, String published_at, String download_url) {
        this.app = app;
        this.version = version;
        this.published_at = published_at;
        this.download_url = download_url;
    }

    public String getApp() {
        return app;
    }

    public String getVersion() {
        return version;
    }

    public String getPublished_at() {
        return published_at;
    }

    public String getDownload_url() {
        return download_url;
    }
}
