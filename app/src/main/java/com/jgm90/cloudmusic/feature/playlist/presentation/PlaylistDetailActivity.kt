package com.jgm90.cloudmusic.feature.playlist.presentation

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.core.ui.theme.CloudMusicTheme
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.feature.playback.presentation.NowPlayingActivity
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@AndroidEntryPoint
class PlaylistDetailActivity : BaseActivity() {
    @Inject
    lateinit var restInterface: RestInterface
    private val viewModel by viewModels<PlaylistViewModel>()

    private var playlistId = 0
    private var playlistName = ""
    private var playlistOffline = 0
    private var playlistCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }

        playlistId = extras.getInt("PLAYLIST_ID")
        playlistName = extras.getString("PLAYLIST_NAME").orEmpty()
        playlistCount = extras.getInt("PLAYLIST_COUNT")
        playlistOffline = extras.getInt("PLAYLIST_OFFLINE")

        setContent {
            CloudMusicTheme {
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    playlistName = playlistName,
                    playlistOffline = playlistOffline,
                    showOfflineToggle = playlistCount > 0,
                    onBack = { finish() },
                    onPlaySong = { index, songs -> openNowPlaying(index, songs) },
                    onDownloadSong = { song -> downloadSongAssets(song) },
                    onDeleteSong = { song ->
                        viewModel.deleteSong(song) {}
                    },
                    onToggleOffline = { offline ->
                        val offlineValue = if (offline) 1 else 0
                        viewModel.updatePlaylistOffline(playlistId, offlineValue) {
                            if (offline) {
                                downloadPlaylist()
                            }
                        }
                    },
                )
            }
        }

        if (playlistOffline == 1) {
            downloadPlaylist()
        }
    }

    private fun openNowPlaying(index: Int, songs: List<SongModel>) {
        val intent = Intent(this, NowPlayingActivity::class.java)
        intent.putExtra("SONG_INDEX", index)
        NowPlayingActivity.audioList = songs.toMutableList()
        startActivity(intent)
    }

    private fun downloadPlaylist() {
        viewModel.loadSongs(playlistId) { songs ->
            if (songs.isNotEmpty()) {
                for (song in songs) {
                    if (!TextUtils.isEmpty(song.local_file)) {
                        val file = File(song.local_file.orEmpty())
                        if (!file.exists()) {
                            downloadSong(song)
                        }
                    } else {
                        downloadSong(song)
                    }
                    if (!TextUtils.isEmpty(song.local_thumbnail)) {
                        val thumbnail = File(song.local_thumbnail.orEmpty())
                        if (!thumbnail.exists()) {
                            downloadThumbnail(song)
                        }
                    } else {
                        downloadThumbnail(song)
                    }
                    if (TextUtils.isEmpty(song.local_lyric)) {
                        downloadLyric(song)
                    }
                }
            }
        }
    }

    private fun downloadSongAssets(song: SongModel) {
        downloadSong(song)
        downloadThumbnail(song)
        downloadLyric(song)
    }

    private fun downloadSong(song: SongModel) {
        CoroutineScope(Dispatchers.IO).launch {
            val songUrl = SharedUtils.server + "play/" + song.id + "/320"
            val filename = song.id + ".mp3"
            val client = OkHttpClient()
            val request = Request.Builder().url(songUrl).build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val inputStream: InputStream = response.body?.byteStream() ?: return@launch
                val directory = File(this@PlaylistDetailActivity.filesDir, "downloads/music")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, filename)

                val outputStream: OutputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                song.local_file = directory.absolutePath + "/" + filename
                viewModel.updateSong(song)
            } else {
                Log.e("App", "Failed to download song(${song.name}): ${response.code}")
            }
        }
    }

    private fun downloadThumbnail(song: SongModel) {
        CoroutineScope(Dispatchers.IO).launch {
            val coverUrl = SharedUtils.server + "pic/" + song.pic_id
            val filename = song.pic_id + ".jpg"
            val client = OkHttpClient()
            val request = Request.Builder().url(coverUrl).build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val inputStream: InputStream = response.body?.byteStream() ?: return@launch
                val directory = File(this@PlaylistDetailActivity.filesDir, "downloads/cover")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, filename)

                val outputStream: OutputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                song.local_thumbnail = directory.absolutePath + "/" + filename
                viewModel.updateSong(song)
            } else {
                Log.e("App", "Failed to download cover(${song.name}): ${response.code}")
            }
        }
    }

    private fun downloadLyric(song: SongModel) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { restInterface.getLyrics(song.lyric_id) }
                .onSuccess { lyric ->
                    if (!lyric?.lyric.isNullOrEmpty()) {
                        song.local_lyric = lyric.lyric
                        viewModel.updateSong(song)
                    } else {
                        Log.e("App", "Bad response")
                    }
                }
                .onFailure {
                    Log.e("App", "Fail to get lyrics")
                }
        }
    }
}
