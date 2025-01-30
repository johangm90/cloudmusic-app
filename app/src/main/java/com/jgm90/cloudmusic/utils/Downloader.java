package com.jgm90.cloudmusic.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class Downloader {

    private boolean destination;
    private int visibility;
    private String url;
    private String name;
    private String filename;
    private Context context;

    public Downloader(Context context) {
        this.context = context;
    }

    public void download(boolean destination, int visibility, String url, String name, String filename) {
        this.destination = destination;
        this.visibility = visibility;
        this.url = url;
        this.name = name;
        this.filename = filename;
        perform_download();
    }

    public void perform_download() {
        Log.i("Url", url);
        try {
            Toast.makeText(context, "Downloading", Toast.LENGTH_LONG).show();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDescription("Cloud Music");
            request.setTitle(name);
            request.allowScanningByMediaScanner();
            if (destination) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            } else {
                File dir = new File(context.getExternalFilesDir(null), "Downloads");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                request.setDestinationInExternalFilesDir(context, null, "/Downloads/" + filename);
            }
            request.setNotificationVisibility(visibility);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
