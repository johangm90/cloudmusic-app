package com.jgm90.cloudmusic.activities

import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.RelativeLayout
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.R2
import com.jgm90.cloudmusic.adapters.SongAdapter
import com.jgm90.cloudmusic.data.PlaylistData
import com.jgm90.cloudmusic.data.SongData
import com.jgm90.cloudmusic.interfaces.DialogCaller
import com.jgm90.cloudmusic.interfaces.OnStartDragListener
import com.jgm90.cloudmusic.listeners.ItemTouchCallback
import com.jgm90.cloudmusic.models.LyricModel
import com.jgm90.cloudmusic.models.SongModel
import com.jgm90.cloudmusic.tables.SongsTable
import com.jgm90.cloudmusic.utils.Divider
import com.jgm90.cloudmusic.utils.RestClient
import com.jgm90.cloudmusic.utils.SharedUtils
import com.jgm90.cloudmusic.widgets.VulgryMessageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class PlaylistDetailActivity : BaseActivity(), DialogCaller, OnStartDragListener {
    @JvmField
    @BindView(R2.id.message_view)
    var message_view: VulgryMessageView? = null

    @JvmField
    @BindView(R2.id.rv_playlist)
    var mRecyclerView: RecyclerView? = null

    @JvmField
    @BindView(R2.id.rl_offline)
    var offlineContainer: RelativeLayout? = null

    @JvmField
    @BindView(R2.id.sb_offline)
    var switchOffline: SwitchCompat? = null
    var search_query: String? = null
    var listState: Parcelable? = null
    private var mAdapter: SongAdapter? = null
    private var mModel: MutableList<SongModel>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    // unbinder
    private val unbinder: Unbinder? = null
    private val searchView: SearchView? = null
    private var id = 0

    private var itemTouchHelper: ItemTouchHelper? = null

    private var dao: SongData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)
        ButterKnife.bind(this)
        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        dao = SongData(this)
        mLayoutManager = LinearLayoutManager(this)
        mRecyclerView!!.setLayoutManager(mLayoutManager)
        mRecyclerView!!.setHasFixedSize(true)
        mRecyclerView!!.addItemDecoration(Divider(this))
        mRecyclerView!!.itemAnimator!!.addDuration = SharedUtils.rv_anim_duration.toLong()
        mRecyclerView!!.setAdapter(null)
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
                offlineContainer!!.visibility = View.GONE
            } else {
                offlineContainer!!.visibility = View.VISIBLE
            }
            if (offline == 1) {
                switchOffline!!.setChecked(true)
                downloadPlaylist()
            } else {
                switchOffline!!.setChecked(false)
            }
        }
        switchOffline!!.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(compoundButton: CompoundButton?, b: Boolean) {
                val dao = PlaylistData(applicationContext)
                val playlist = dao.getOne("playlist_id=$id") ?: return
                if (b) {
                    playlist.offline = 1
                    dao.update(playlist)
                    downloadPlaylist()
                } else {
                    playlist.offline = 0
                    dao.update(playlist)
                }
            }
        })
    }

    private fun downloadPlaylist() {
        val songs = dao!!.getAllFilter(SongsTable.COL_PLAYLIST_ID + "=" + id)
        if (songs.isNotEmpty()) {
            for (i in songs.indices) {
                if (!TextUtils.isEmpty(songs[i].local_file)) {
                    val song = File(songs[i].local_file)
                    if (!song.exists()) {
                        downloadSong(songs[i])
                    }
                } else {
                    downloadSong(songs[i])
                }
                if (!TextUtils.isEmpty(songs[i].local_thumbnail)) {
                    val thumbnail = File(songs[i].local_thumbnail)
                    if (!thumbnail.exists()) {
                        downloadThumbnail(songs[i])
                    }
                } else {
                    downloadThumbnail(songs[i])
                }
                if (TextUtils.isEmpty(songs[i].local_lyric)) {
                    downloadLyric(songs[i])
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
                dao!!.update(song)
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
                dao!!.update(song)
            } else {
                Log.e("App", "Failed to download cover(${song.name}): ${response.code}")
            }
        }
    }

    fun downloadLyric(song: SongModel) {
        val api = RestClient.build(SharedUtils.server)
        var lyrics_call: Call<LyricModel?>
        lyrics_call = api.getLyrics(song.lyric_id)
        lyrics_call.enqueue(object : Callback<LyricModel?> {
            override fun onResponse(call: Call<LyricModel?>?, response: Response<LyricModel?>?) {
                if (response!!.isSuccessful) {
                    song.local_lyric = response.body()!!.lyric
                    dao!!.update(song)
                } else {
                    Log.e("App", "Bad response")
                }
            }

            override fun onFailure(call: Call<LyricModel?>?, t: Throwable?) {
                Log.e("App", "Fail to get lyrics")
            }
        })
    }

    private fun setUpTouch() {
        val adapter = mAdapter ?: return
        val callback: ItemTouchHelper.Callback = ItemTouchCallback(adapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper!!.attachToRecyclerView(mRecyclerView)
    }

    public override fun onResume() {
        super.onResume()
        if (listState != null) {
            mLayoutManager!!.onRestoreInstanceState(listState)
        }
    }

    fun reload(id: Int) {
        message_view!!.visibility = View.GONE
        mModel!!.clear()
        mRecyclerView!!.setAdapter(null)
        getSongs(id)
    }

    fun getSongs(id: Int) {
        try {
            val model = dao!!.getAllFilter(SongsTable.COL_PLAYLIST_ID + "=" + id).toMutableList()
            mModel = model
            if (model.isNotEmpty()) {
                mAdapter = SongAdapter(model, this, this, this)
                mRecyclerView!!.setAdapter(mAdapter)
                mAdapter!!.notifyItemChanged(0)
                setUpTouch()
            } else {
                SharedUtils.showMessage(
                    message_view,
                    R.drawable.ic_info_black_24dp,
                    R.string.no_songs
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
