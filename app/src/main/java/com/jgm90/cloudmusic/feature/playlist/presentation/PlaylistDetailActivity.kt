package com.jgm90.cloudmusic.feature.playlist.presentation

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.innertube.YouTubeRepository
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.playback.PlaybackController
import com.jgm90.cloudmusic.core.ui.theme.CloudMusicTheme
import com.jgm90.cloudmusic.feature.playback.presentation.NowPlayingActivity
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@AndroidEntryPoint
class PlaylistDetailActivity : BaseActivity() {
    @Inject
    lateinit var youTubeRepository: YouTubeRepository
    @Inject
    lateinit var playbackController: PlaybackController
    @Inject
    lateinit var httpClient: OkHttpClient

    private val viewModel by viewModels<PlaylistViewModel>()

    private var playlistId = 0
    private var playlistName = ""
    private var playlistOffline = 0
    private var playlistCount = 0

    private val downloadLimiter = Semaphore(2)
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        playbackController.setQueue(songs, index)
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
        val songId = song.id ?: return

        downloadScope.launch {
            downloadLimiter.withPermit {
                // Get the stream URL from YouTube
                val streamUrl = youTubeRepository.getStreamUrl(songId)
                if (streamUrl == null) {
                    Log.e("App", "Failed to get stream URL for song: ${song.name}")
                    return@withPermit
                }

                val filename = songId + ".mp3"
                val request = Request.Builder().url(streamUrl).build()

                runCatching {
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val inputStream: InputStream = response.body?.byteStream() ?: return@runCatching
                        val directory = File(this@PlaylistDetailActivity.filesDir, "downloads/music")
                        if (!directory.exists()) {
                            directory.mkdirs()
                        }
                        val file = File(directory, filename)

                        val outputStream: OutputStream = FileOutputStream(file)
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }

                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        song.local_file = directory.absolutePath + "/" + filename
                        viewModel.updateSong(song)
                        Log.d("App", "Downloaded song: ${song.name}")
                    } else {
                        Log.e("App", "Failed to download song(${song.name}): ${response.code}")
                    }
                }.onFailure { error ->
                    Log.e("App", "Error downloading song(${song.name}): ${error.message}")
                }
            }
        }
    }

    private fun downloadThumbnail(song: SongModel) {
        downloadScope.launch {
            downloadLimiter.withPermit {
                // Use getCoverThumbnail which handles both album art URLs and video thumbnails
                val coverUrl = song.getCoverThumbnail()
                if (coverUrl.isEmpty()) {
                    Log.d("App", "No thumbnail URL for: ${song.name}")
                    return@withPermit
                }

                // Generate filename from video ID for consistency
                val filename = (song.id ?: return@withPermit) + ".jpg"
                val request = Request.Builder().url(coverUrl).build()

                runCatching {
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val inputStream: InputStream = response.body?.byteStream() ?: return@runCatching
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
                        Log.d("App", "Downloaded thumbnail: ${song.name}")
                    } else {
                        Log.e("App", "Failed to download cover(${song.name}): ${response.code}")
                    }
                }.onFailure { error ->
                    Log.e("App", "Error downloading cover(${song.name}): ${error.message}")
                }
            }
        }
    }

    private fun downloadLyric(song: SongModel) {
        val lyricId = song.lyric_id ?: return

        downloadScope.launch {
            downloadLimiter.withPermit {
                // Get lyrics from LRCLIB via YouTubeRepository
                val lyricModel = youTubeRepository.getLyrics(lyricId)
                if (lyricModel != null && !lyricModel.lyric.isNullOrEmpty()) {
                    song.local_lyric = lyricModel.lyric
                    viewModel.updateSong(song)
                    Log.d("App", "Downloaded lyrics for: ${song.name}")
                } else {
                    Log.d("App", "No lyrics found for: ${song.name}")
                }
            }
        }
    }

    override fun onDestroy() {
        downloadScope.cancel()
        super.onDestroy()
    }
}
