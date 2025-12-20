package com.jgm90.cloudmusic.feature.playback.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jgm90.cloudmusic.GlideApp
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.databinding.ActivityNowPlayingBinding
import com.jgm90.cloudmusic.feature.playback.presentation.adapter.InfinitePagerAdapter
import com.jgm90.cloudmusic.feature.playback.presentation.adapter.SlidePagerAdapter
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.core.model.LyricModel
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import com.jgm90.cloudmusic.core.ui.animation.AnimationUtils
import com.jgm90.cloudmusic.core.playback.LyricLine
import com.jgm90.cloudmusic.core.playback.Lyrics
import com.jgm90.cloudmusic.core.playback.PlaybackMode
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.feature.playback.presentation.widget.CMViewPager
import com.jgm90.cloudmusic.feature.playback.presentation.widget.PlayPauseDrawable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class NowPlayingActivity : BaseActivity() {
    @Inject
    lateinit var restInterface: RestInterface

    private lateinit var binding: ActivityNowPlayingBinding
    var pager: CMViewPager? = null
    var songtitle: TextView? = null
    var songartist: TextView? = null
    var mProgress: SeekBar? = null
    var elapsedtime: TextView? = null
    var songduration: TextView? = null
    var shuffle: ImageView? = null
    var previous: ImageView? = null
    var playPauseFloating: FloatingActionButton? = null
    var next: ImageView? = null
    var repeat: ImageView? = null
    var header_view: CoordinatorLayout? = null

    private var lyricsJob: Job? = null

    private val updateProgress = object : Runnable {
        override fun run() {
            val service = player_service
            if (service != null && service.isPlaying() && mProgress != null) {
                songduration?.text = SharedUtils.makeShortTimeString(applicationContext, service.duration() / 1000)
                mProgress?.max = service.duration().toInt()
                val position = service.getPosition()
                mProgress?.progress = position.toInt()
                if (elapsedtime != null) {
                    elapsedtime?.text = SharedUtils.makeShortTimeString(applicationContext, position / 1000)
                }
            }
            progressHandler.postDelayed(this, 50)
        }
    }

    private var slideAdapter: SlidePagerAdapter? = null
    private var infiniteAdapter: InfinitePagerAdapter? = null
    var song_index = 0
    private var song: SongModel? = null
    private val playPauseDrawable = PlayPauseDrawable()
    private var lyricsView: RelativeLayout? = null
    private var currentLine: TextView? = null
    private var nextLine: TextView? = null
    private var lyricModel: LyricModel? = null
    private var lyrics: List<LyricLine>? = null
    private var current_line = 0
    private var lastPosition = 0
    private var eventJobs: List<Job> = emptyList()

    private val updateLyrics = object : Runnable {
        override fun run() {
            val service = player_service
            if (service != null && service.isPlaying() && currentLine != null) {
                val position = service.getPosition()
                if (position > 0 && lyrics != null) {
                    val lyricsList = lyrics ?: emptyList()
                    for (i in lyricsList.indices) {
                        val pos = (lyricsList[i].getTime() * 1000).toInt()
                        if (pos <= position && current_line == 0) {
                            if (lyricsList[i].lyric != "") {
                                currentLine?.text = lyricsList[i].lyric.replace("&apos;", "'")
                                animateLyric()
                            }
                            nextLine?.text = lyricsList[i + 1].lyric.replace("&apos;", "'")
                        }
                        if (pos <= position && i > current_line) {
                            if (lyricsList[i].lyric != "") {
                                currentLine?.text = lyricsList[i].lyric.replace("&apos;", "'")
                                animateLyric()
                            }
                            current_line = i
                            if (i + 1 < lyricsList.size) {
                                nextLine?.text = lyricsList[i + 1].lyric.replace("&apos;", "'")
                            } else {
                                nextLine?.text = ""
                            }
                        }
                    }
                }
            }
            lyricsHandler.postDelayed(this, 50)
        }
    }

    private lateinit var mainHandler: Handler
    private lateinit var progressHandler: Handler
    private lateinit var lyricsHandler: Handler

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MediaPlayerService.LocalBinder
            player_service = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }

    private val loadInfo = Runnable {
        val list = audioList
        if (song_index in list.indices) {
            getDetail(list[song_index].id)
            getLyrics(list[song_index].id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNowPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pager = binding.pager
        songtitle = binding.songTitle
        songartist = binding.songArtist
        mProgress = binding.songProgress
        elapsedtime = binding.songElapsedTime
        songduration = binding.songDuration
        shuffle = binding.shuffle
        previous = binding.previous
        playPauseFloating = binding.playpausefloating
        next = binding.next
        repeat = binding.repeat
        header_view = binding.headerViewA
        setupToolbar()
        binding.playpausefloating.setOnClickListener { playOrPause() }
        binding.previous.setOnClickListener { skipToPrevious() }
        binding.next.setOnClickListener { skipToNext() }
        binding.shuffle.setOnClickListener { setShuffle() }
        binding.repeat.setOnClickListener { setRepeatMode() }
        val extras = intent.extras
        mainHandler = Handler()
        progressHandler = Handler()
        lyricsHandler = Handler()
        if (SharedUtils.isMyServiceRunning(this, MediaPlayerService::class.java)) {
            val playerIntent = Intent(this, MediaPlayerService::class.java)
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        slideAdapter = SlidePagerAdapter(this, audioList)
        if (SharedUtils.getRepeatMode(this) == PlaybackMode.REPEAT) {
            infiniteAdapter = InfinitePagerAdapter(slideAdapter!!)
            pager?.adapter = infiniteAdapter
        } else {
            pager?.adapter = slideAdapter
        }
        song_index = extras?.getInt("SONG_INDEX") ?: MediaPlayerService.audioIndex
        lastPosition = pager?.currentItem ?: 0
        pager?.setCurrentItem(song_index)
        if (savedInstanceState != null) {
            song = savedInstanceState.getParcelable(SONG_OBJECT)
            lyricModel = savedInstanceState.getParcelable(LYRICS_OBJECT)
            if (song != null) {
                setDetails()
            } else {
                getDetail(audioList[song_index].id)
            }
            if (lyricModel != null) {
                if (lyricModel?.lyric != null) {
                    lyricModel?.lyric?.let { lrc ->
                        lyrics = Lyrics.parse(lrc)
                    }
                    if (!lyrics.isNullOrEmpty()) {
                        lyricsHandler.postDelayed(updateLyrics, 10)
                    } else {
                        currentLine?.text = "No lyrics found"
                        nextLine?.text = ""
                    }
                } else {
                    currentLine?.text = "No lyrics found"
                    nextLine?.text = ""
                }
            } else {
                getLyrics(audioList[song_index].id)
            }
            lyricsView?.visibility = View.VISIBLE
        } else {
            if (extras != null) {
                song_index = extras.getInt("SONG_INDEX")
                playAudio()
                MediaPlayerService.audioList = audioList
                MediaPlayerService.audioIndex = song_index
            } else {
                song_index = MediaPlayerService.audioIndex
                song = MediaPlayerService.song
                setDetails()
                getLyrics(audioList[song_index].id)
            }
        }
        pager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                Log.d("last", lastPosition.toString())
                Log.d("pos", position.toString())
                if (lastPosition > position) {
                    skipToPrevious()
                } else if (lastPosition < position) {
                    skipToNext()
                }
                lastPosition = position
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        AppEventBus.clearSticky(OnSourceChangeEvent::class)
        init()
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            val ab: ActionBar? = supportActionBar
            ab?.setDisplayHomeAsUpEnabled(true)
            ab?.title = ""
            toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_down_24dp)
        }
    }

    private fun init() {
        if (SharedUtils.getShuffle(this)) {
            shuffle?.alpha = 1f
        } else {
            shuffle?.alpha = 0.4f
        }
        when (SharedUtils.getRepeatMode(this)) {
            PlaybackMode.NORMAL -> {
                repeat?.setImageResource(R.drawable.ic_repeat_black_24dp)
                repeat?.alpha = 0.4f
            }
            PlaybackMode.REPEAT -> {
                repeat?.setImageResource(R.drawable.ic_repeat_black_24dp)
                repeat?.alpha = 1f
            }
            PlaybackMode.REPEAT_ONE -> {
                repeat?.setImageResource(R.drawable.ic_repeat_one_black_24dp)
                repeat?.alpha = 1f
            }
        }
    }

    private fun animateLyric() {
        val animation: Animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up)
        animation.reset()
        currentLine?.clearAnimation()
        currentLine?.startAnimation(animation)
    }

    fun playOrPause() {
        mainHandler.postDelayed({
            player_service?.eventPlayOrPause(PlayPauseEvent("From Now Playing Activity"))
        }, 250)
    }

    override fun onPause() {
        super.onPause()
        progressHandler.removeCallbacks(updateProgress)
        lyricsHandler.removeCallbacks(updateLyrics)
    }

    override fun onResume() {
        super.onResume()
        if (player_service?.isPlaying() == true) {
            progressHandler.post(updateProgress)
        }
        if (lyricModel?.lyric != null) {
            lyricsHandler.post(updateLyrics)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lyricsJob?.cancel()
        progressHandler.removeCallbacks(updateProgress)
        lyricsHandler.removeCallbacks(updateLyrics)
        unbindService(serviceConnection)
    }

    override fun onStart() {
        super.onStart()
        eventJobs = listOf(
            AppEventBus.observe<IsPlayingEvent>(lifecycleScope) { setIcon(it) },
            AppEventBus.observe<OnSourceChangeEvent>(lifecycleScope) { setInfo(it) },
        )
    }

    override fun onStop() {
        super.onStop()
        eventJobs.forEach { it.cancel() }
        eventJobs = emptyList()
    }

    fun setIcon(event: IsPlayingEvent) {
        if (event.isPlaying) {
            playPauseDrawable.transformToPause(true)
        } else {
            playPauseDrawable.transformToPlay(true)
        }
    }

    fun setInfo(event: OnSourceChangeEvent) {
        Log.d("Event", event.message)
        resetLyricsView()
        song_index = player_service?.current_index() ?: song_index
        mainHandler.post(loadInfo)
    }

    private fun resetLyricsView() {
        if (currentLine != null && nextLine != null) {
            current_line = 0
            currentLine?.text = ""
            nextLine?.text = ""
        }
    }

    private fun getDetail(id: String?) {
        song = audioList[song_index]
        if (song != null) {
            setDetails()
        }
    }

    private fun getLyrics(id: String?) {
        song = audioList[song_index]
        val currentSong = song ?: return
        lyricsView = pager?.findViewWithTag("lyricsView_${currentSong.id}")
        currentLine = pager?.findViewWithTag("currentView_${currentSong.id}")
        nextLine = pager?.findViewWithTag("nextView_${currentSong.id}")
        if (lyricsView != null) {
            if (!TextUtils.isEmpty(currentSong.local_lyric)) {
                lyricModel = LyricModel(1, 1, currentSong.local_lyric, 200)
                lyricModel?.lyric?.let { lrc ->
                    lyrics = Lyrics.parse(lrc)
                }
                if (!lyrics.isNullOrEmpty()) {
                    lyricsHandler.postDelayed(updateLyrics, 10)
                    lyricsView?.visibility = View.VISIBLE
                }
            } else {
                lyricsJob?.cancel()
                lyricsJob = lifecycleScope.launch {
                    val response = runCatching {
                        withContext(Dispatchers.IO) {
                            restInterface.getLyrics(id)
                        }
                    }
                    response.onSuccess { lyric ->
                        lyricModel = lyric
                        if (!lyricModel?.lyric.isNullOrEmpty()) {
                            Log.i("Lyrics", lyricModel?.lyric.orEmpty())
                            lyricModel?.lyric?.let { lrc ->
                                lyrics = Lyrics.parse(lrc)
                            }
                            lyricsHandler.postDelayed(updateLyrics, 10)
                        } else {
                            currentLine?.text = "No lyrics found"
                            nextLine?.text = ""
                        }
                        lyricsView?.visibility = View.VISIBLE
                    }.onFailure {
                        Log.e("App", "Fail to get lyrics")
                    }
                }
            }
        }
    }

    private fun getMostPopulousSwatch(palette: Palette?): Palette.Swatch? {
        var mostPopulous: Palette.Swatch? = null
        if (palette != null) {
            for (swatch in palette.swatches) {
                if (mostPopulous == null || swatch.population > mostPopulous.population) {
                    mostPopulous = swatch
                }
            }
        }
        return mostPopulous
    }

    private fun setUpBackgroundColor(fl: CoordinatorLayout?, palette: Palette?) {
        val swatch = getMostPopulousSwatch(palette)
        if (swatch != null && fl != null) {
            val startColor = ContextCompat.getColor(fl.context, R.color.grey_800)
            val endColor = swatch.rgb
            AnimationUtils.animColorChange(fl, startColor, endColor)
        }
    }

    private fun setDetails() {
        song = audioList[song_index]
        val currentSong = song ?: return
        val picUrl = if (!TextUtils.isEmpty(currentSong.local_thumbnail)) {
            currentSong.local_thumbnail
        } else {
            SharedUtils.server + "pic/" + currentSong.pic_id
        }
        lastPosition = song_index
        if (SharedUtils.getShuffle(this)) {
            pager?.setCurrentItem(song_index, false)
        } else {
            pager?.setCurrentItem(song_index, true)
        }
        GlideApp.with(applicationContext)
            .asBitmap()
            .load(picUrl)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Palette.from(resource).generate { palette ->
                        setUpBackgroundColor(header_view, palette)
                    }
                }
            })
        songtitle?.text = currentSong.name
        songtitle?.isSelected = true
        songartist?.text = TextUtils.join(", ", currentSong.artist)
        if (playPauseFloating != null) {
            playPauseFloating?.setImageDrawable(playPauseDrawable)
            playPauseDrawable.transformToPause(false)
        }
        if (songduration != null) {
            val service = player_service
            if (service != null && service.isPlaying()) {
                songduration?.text = SharedUtils.makeShortTimeString(applicationContext, service.duration() / 1000)
            }
        }
        if (mProgress != null) {
            val service = player_service
            if (service != null && service.isPlaying()) {
                mProgress?.max = service.duration().toInt()
            }
            progressHandler.removeCallbacks(updateProgress)
            progressHandler.postDelayed(updateProgress, 10)
        }
        setSeekBarListener()
    }

    fun skipToPrevious() {
        mainHandler.postDelayed({
            player_service?.skipToPrevious()
            song_index = player_service?.current_index() ?: song_index
            mainHandler.postDelayed(loadInfo, 10)
            if (SharedUtils.getRepeatMode(applicationContext) != PlaybackMode.REPEAT && song_index == 0) {
                previous?.isEnabled = false
                previous?.alpha = 0.4f
            } else {
                next?.isEnabled = true
                next?.alpha = 1f
            }
        }, 200)
    }

    fun skipToNext() {
        mainHandler.postDelayed({
            player_service?.skipToNext()
            song_index = player_service?.current_index() ?: song_index
            mainHandler.postDelayed(loadInfo, 10)
            if (SharedUtils.getRepeatMode(applicationContext) != PlaybackMode.REPEAT &&
                song_index == audioList.size - 1
            ) {
                next?.isEnabled = false
                next?.alpha = 0.4f
            } else {
                previous?.isEnabled = true
                previous?.alpha = 1f
            }
        }, 200)
    }

    fun setShuffle() {
        if (SharedUtils.getShuffle(this)) {
            shuffle?.alpha = 0.4f
            SharedUtils.setShuffle(this, false)
        } else {
            shuffle?.alpha = 1f
            SharedUtils.setShuffle(this, true)
        }
    }

    fun setRepeatMode() {
        when (SharedUtils.getRepeatMode(this)) {
            PlaybackMode.NORMAL -> {
                repeat?.setImageResource(R.drawable.ic_repeat_black_24dp)
                repeat?.alpha = 1f
                SharedUtils.setRepeatMode(this, PlaybackMode.REPEAT)
                player_service?.setLoop(false)
            }
            PlaybackMode.REPEAT -> {
                repeat?.setImageResource(R.drawable.ic_repeat_one_black_24dp)
                repeat?.alpha = 1f
                SharedUtils.setRepeatMode(this, PlaybackMode.REPEAT_ONE)
                player_service?.setLoop(true)
            }
            PlaybackMode.REPEAT_ONE -> {
                repeat?.setImageResource(R.drawable.ic_repeat_black_24dp)
                repeat?.alpha = 0.4f
                SharedUtils.setRepeatMode(this, PlaybackMode.NORMAL)
                player_service?.setLoop(false)
            }
        }
    }

    private fun setSeekBarListener() {
        mProgress?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b) {
                    resetLyricsView()
                    player_service?.seek(i)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("serviceStatus")
    }

    override fun onServiceAttached(service: MediaPlayerService) {
        super.onServiceAttached(service)
        serviceBound = true
    }

    private fun playAudio() {
        if (!serviceBound) {
            startService(Intent(this, MediaPlayerService::class.java))
        } else {
            val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            supportFinishAfterTransition()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val Broadcast_PLAY_NEW_AUDIO = "com.jgm90.cloudmusic.PlayNewAudio"
        const val SONG_OBJECT = "song_model"
        const val LYRICS_OBJECT = "lyrics_model"

        @JvmField
        var audioList: MutableList<SongModel> = mutableListOf()
    }
}
