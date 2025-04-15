package com.jgm90.cloudmusic.activities;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.fragments.PlaybackControlsFragment;
import com.jgm90.cloudmusic.services.MediaPlayerService;
import com.jgm90.cloudmusic.utils.DbHelper;

public class BaseActivity extends AppCompatActivity implements ServiceConnection {

    protected MediaPlayerService player_service;
    protected boolean serviceBound = false;

    private PlaybackControlsFragment mControlsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        new DbHelper(this);
        attachService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Base", "OnStart");
        mControlsFragment = (PlaybackControlsFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);
        if (mControlsFragment == null) {
            //throw new IllegalStateException("Mising fragment with id 'controls'. Cannot continue.");
        } else {
            if (player_service == null) {
                hidePlaybackControls();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player_service != null && player_service.current_index() != -1) {
            showPlaybackControls();
        }
    }

    protected void showPlaybackControls() {
        Log.d("Base", "showPlaybackControls");
        mControlsFragment = (PlaybackControlsFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_playback_controls);
        if (mControlsFragment != null) {
            getFragmentManager().beginTransaction()
                    .show(mControlsFragment)
                    .commit();
        }
    }

    protected void hidePlaybackControls() {
        Log.d("Base", "hidePlaybackControls");
        getFragmentManager().beginTransaction()
                .hide(mControlsFragment)
                .commit();
    }

    private void attachService() {
        Intent service = new Intent(this, MediaPlayerService.class);
        bindService(service, this, Service.BIND_AUTO_CREATE);
    }

    private void detachService() {
        unbindService(this);
    }

    protected void onServiceAttached(MediaPlayerService service) {
        // do something necessary by its subclass.
        if (service.isPlaying()) {
            showPlaybackControls();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MediaPlayerService.LocalBinder serviceBinder = (MediaPlayerService.LocalBinder) service;
        player_service = serviceBinder.getService();
        serviceBound = true;
        onServiceAttached(player_service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        serviceBound = false;
        player_service = null;
    }

    @Override
    protected void onDestroy() {
        detachService();
        super.onDestroy();
    }
}
