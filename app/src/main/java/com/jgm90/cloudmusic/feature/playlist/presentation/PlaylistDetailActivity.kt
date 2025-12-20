package com.jgm90.cloudmusic.feature.playlist.presentation

import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.databinding.ActivityPlaylistDetailBinding
import com.jgm90.cloudmusic.feature.playlist.presentation.adapter.SongAdapter
import com.jgm90.cloudmusic.feature.playlist.data.PlaylistData
import com.jgm90.cloudmusic.feature.playlist.data.SongData
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.DialogCaller
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.OnStartDragListener
import com.jgm90.cloudmusic.feature.playlist.presentation.listener.ItemTouchCallback
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.ui.decoration.Divider
import com.jgm90.cloudmusic.core.util.SharedUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@AndroidEntryPoint
class PlaylistDetailActivity : BaseActivity(), DialogCaller, OnStartDragListener {
    private lateinit var binding: ActivityPlaylistDetailBinding
    private val contentBinding get() = binding.playlistDetail
    @Inject
    lateinit var restInterface: RestInterface
    var search_query: String? = null
    var listState: Parcelable? = null
    private var mAdapter: SongAdapter? = null
    private var mModel: MutableList<SongModel>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    private var id = 0

    private var itemTouchHelper: ItemTouchHelper? = null

    private var dao: SongData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        dao = SongData(this)
        mLayoutManager = LinearLayoutManager(this)
        contentBinding.rvPlaylist.layoutManager = mLayoutManager
        contentBinding.rvPlaylist.setHasFixedSize(true)
        contentBinding.rvPlaylist.addItemDecoration(Divider(this))
        contentBinding.rvPlaylist.itemAnimator?.addDuration = SharedUtils.rv_anim_duration.toLong()
        contentBinding.rvPlaylist.adapter = null
        mModel = ArrayList()
        val extras = intent.extras
        if (extras != null) {
            id = extras.getInt("PLAYLIST_ID")
            val name = extras.getString("PLAYLIST_NAME")
            val count = extras.getInt("PLAYLIST_COUNT")
            val offline = extras.getInt("PLAYLIST_OFFLINE")
            reload(id)
            title = name
            if (count == 0) {
                contentBinding.rlOffline.visibility = View.GONE
            } else {
                contentBinding.rlOffline.visibility = View.VISIBLE
            }
            if (offline == 1) {
                contentBinding.sbOffline.isChecked = true
                downloadPlaylist()
            } else {
                contentBinding.sbOffline.isChecked = false
            }
        }
        contentBinding.sbOffline.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, b: Boolean) {
                val playlistDao = PlaylistData(applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    val playlist = playlistDao.getById(id) ?: return@launch
                    if (b) {
                        playlist.offline = 1
                        playlistDao.update(playlist)
                        downloadPlaylist()
                    } else {
                        playlist.offline = 0
                        playlistDao.update(playlist)
                    }
                }
            }
        })
    }

    private fun downloadPlaylist() {
        val localDao = dao ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val songs = localDao.getAllByPlaylist(id)
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

    fun downloadSong(song: SongModel) {
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
                dao?.update(song)
            } else {
                Log.e("App", "Failed to download song(${song.name}): ${response.code}")
            }
        }
    }

    fun downloadThumbnail(song: SongModel) {
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
                dao?.update(song)
            } else {
                Log.e("App", "Failed to download cover(${song.name}): ${response.code}")
            }
        }
    }

    fun downloadLyric(song: SongModel) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { restInterface.getLyrics(song.lyric_id) }
                .onSuccess { lyric ->
                    if (!lyric?.lyric.isNullOrEmpty()) {
                        song.local_lyric = lyric.lyric
                        dao?.update(song)
                    } else {
                        Log.e("App", "Bad response")
                    }
                }
                .onFailure {
                    Log.e("App", "Fail to get lyrics")
                }
        }
    }

    private fun setUpTouch() {
        val adapter = mAdapter ?: return
        val callback: ItemTouchHelper.Callback = ItemTouchCallback(adapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper!!.attachToRecyclerView(contentBinding.rvPlaylist)
    }

    public override fun onResume() {
        super.onResume()
        if (listState != null) {
            mLayoutManager!!.onRestoreInstanceState(listState)
        }
    }

    fun reload(id: Int) {
        contentBinding.messageView.visibility = View.GONE
        mModel?.clear()
        contentBinding.rvPlaylist.adapter = null
        getSongs(id)
    }

    fun getSongs(id: Int) {
        val localDao = dao ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val model = localDao.getAllByPlaylist(id).toMutableList()
            withContext(Dispatchers.Main) {
                mModel = model
                if (model.isNotEmpty()) {
                    mAdapter = SongAdapter(model, this@PlaylistDetailActivity, this@PlaylistDetailActivity, this@PlaylistDetailActivity)
                    contentBinding.rvPlaylist.adapter = mAdapter
                    mAdapter!!.notifyItemChanged(0)
                    setUpTouch()
                } else {
                    SharedUtils.showMessage(
                        contentBinding.messageView,
                        R.drawable.ic_info_black_24dp,
                        R.string.no_songs
                    )
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPositiveCall() {
        reload(id)
    }

    override fun onDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper!!.startDrag(viewHolder)
    }
}
