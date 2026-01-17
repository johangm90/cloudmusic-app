package com.jgm90.cloudmusic.feature.playback.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.ui.theme.CloudMusicTheme
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.feature.playback.presentation.screen.NowPlayingScreen
import com.jgm90.cloudmusic.feature.playback.presentation.viewmodel.NowPlayingViewModel
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NowPlayingActivity : BaseActivity() {

    private val viewModel: NowPlayingViewModel by viewModels()

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MediaPlayerService.LocalBinder
            player_service = binder.getService()
            serviceBound = true
            viewModel.playerService = player_service
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
            viewModel.playerService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.updateAudioPermission(hasAudioPermission())
        requestAudioPermission()

        if (SharedUtils.isMyServiceRunning(this, MediaPlayerService::class.java)) {
            val playerIntent = Intent(this, MediaPlayerService::class.java)
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        if (audioList.isEmpty() && MediaPlayerService.audioList.isNotEmpty()) {
            audioList = MediaPlayerService.audioList
        }

        val extras = intent.extras
        if (extras != null) {
            val songIndex = extras.getInt("SONG_INDEX", MediaPlayerService.audioIndex)
            playAudio()
            MediaPlayerService.audioList = audioList
            MediaPlayerService.audioIndex = songIndex
            viewModel.loadSongInfo(songIndex, audioList)
        } else {
            val songIndex = MediaPlayerService.audioIndex
            viewModel.loadSongInfo(songIndex, audioList)
        }

        setContent {
            CloudMusicTheme {
                NowPlayingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                )
            }
        }
    }

    private fun requestAudioPermission() {
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            viewModel.updateAudioPermission(hasAudioPermission())
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopProgressUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (player_service?.isPlaying() == true) {
            viewModel.startProgressUpdates()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startEventObservation()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopEventObservation()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("serviceStatus")
    }

    override fun onServiceAttached(service: MediaPlayerService) {
        super.onServiceAttached(service)
        serviceBound = true
        viewModel.playerService = service
    }

    private fun playAudio() {
        if (!serviceBound) {
            startService(Intent(this, MediaPlayerService::class.java))
        } else {
            val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
        }
    }

    companion object {
        const val Broadcast_PLAY_NEW_AUDIO = "com.jgm90.cloudmusic.PlayNewAudio"

        @JvmField
        var audioList: MutableList<SongModel> = mutableListOf()
    }
}
