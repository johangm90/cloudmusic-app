package com.jgm90.cloudmusic.core.app

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import androidx.compose.runtime.mutableStateOf

open class BaseActivity : AppCompatActivity(), ServiceConnection {
    protected var player_service: MediaPlayerService? = null
    protected var serviceBound = false

    protected val playbackControlsVisible = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#0B1118")
        window.navigationBarColor = android.graphics.Color.parseColor("#0B1118")
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        volumeControlStream = AudioManager.STREAM_MUSIC
        attachService()
    }

    override fun onStart() {
        super.onStart()
        Log.d("Base", "OnStart")
        if (player_service == null) {
            playbackControlsVisible.value = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (player_service != null && player_service?.current_index() != -1) {
            playbackControlsVisible.value = true
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
            playbackControlsVisible.value = true
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
