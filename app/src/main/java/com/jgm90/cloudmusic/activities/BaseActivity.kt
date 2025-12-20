package com.jgm90.cloudmusic.activities

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.fragments.PlaybackControlsFragment
import com.jgm90.cloudmusic.services.MediaPlayerService
import com.jgm90.cloudmusic.utils.DbHelper

open class BaseActivity : AppCompatActivity(), ServiceConnection {
    protected var player_service: MediaPlayerService? = null
    protected var serviceBound = false

    private var controlsFragment: PlaybackControlsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        DbHelper.getInstance(this)
        attachService()
    }

    override fun onStart() {
        super.onStart()
        Log.d("Base", "OnStart")
        controlsFragment = fragmentManager.findFragmentById(R.id.fragment_playback_controls) as? PlaybackControlsFragment
        if (controlsFragment != null) {
            if (player_service == null) {
                hidePlaybackControls()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (player_service != null && player_service?.current_index() != -1) {
            showPlaybackControls()
        }
    }

    protected fun showPlaybackControls() {
        Log.d("Base", "showPlaybackControls")
        controlsFragment = fragmentManager.findFragmentById(R.id.fragment_playback_controls) as? PlaybackControlsFragment
        controlsFragment?.let {
            fragmentManager.beginTransaction().show(it).commit()
        }
    }

    protected fun hidePlaybackControls() {
        Log.d("Base", "hidePlaybackControls")
        controlsFragment?.let {
            fragmentManager.beginTransaction().hide(it).commit()
        }
    }

    private fun attachService() {
        val service = Intent(this, MediaPlayerService::class.java)
        bindService(service, this, Service.BIND_AUTO_CREATE)
    }

    private fun detachService() {
        unbindService(this)
    }

    protected open fun onServiceAttached(service: MediaPlayerService) {
        if (service.isPlaying()) {
            showPlaybackControls()
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val serviceBinder = service as MediaPlayerService.LocalBinder
        player_service = serviceBinder.getService()
        serviceBound = true
        player_service?.let { onServiceAttached(it) }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        serviceBound = false
        player_service = null
    }

    override fun onDestroy() {
        detachService()
        super.onDestroy()
    }
}
