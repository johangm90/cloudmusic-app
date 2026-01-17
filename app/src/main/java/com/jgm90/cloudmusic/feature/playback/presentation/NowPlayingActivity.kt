package com.jgm90.cloudmusic.feature.playback.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.core.model.LyricModel
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.core.playback.LyricLine
import com.jgm90.cloudmusic.core.playback.Lyrics
import com.jgm90.cloudmusic.core.playback.PlaybackMode
import com.jgm90.cloudmusic.core.ui.theme.CloudMusicTheme
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.core.data.local.repository.LibraryRepository
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

@AndroidEntryPoint
class NowPlayingActivity : BaseActivity() {
    @Inject
    lateinit var restInterface: RestInterface

    private val songTitle = mutableStateOf("")
    private val songArtist = mutableStateOf("")
    private val coverUrl = mutableStateOf("")
    private val isPlaying = mutableStateOf(false)
    private val shuffleEnabled = mutableStateOf(false)
    private val repeatMode = mutableStateOf(PlaybackMode.NORMAL)
    private val progressMs = mutableStateOf(0)
    private val durationMs = mutableStateOf(0)
    private val elapsedText = mutableStateOf("0:00")
    private val durationText = mutableStateOf("0:00")
    private val currentLyric = mutableStateOf("")
    private val nextLyric = mutableStateOf("")
    private val isLiked = mutableStateOf(false)

    private var userSeeking = false
    private var lyricsJob: Job? = null
    private var lyrics: List<LyricLine>? = null
    private var currentLineIndex = 0
    private var songIndex = 0
    private var song: SongModel? = null
    private var eventJobs: List<Job> = emptyList()

    private lateinit var mainHandler: Handler
    private lateinit var progressHandler: Handler
    private lateinit var lyricsHandler: Handler
    @Inject
    lateinit var libraryRepository: LibraryRepository

    private val updateProgress = object : Runnable {
        override fun run() {
            val service = player_service
            if (service != null && service.isPlaying()) {
                durationMs.value = service.duration().toInt()
                durationText.value = SharedUtils.makeShortTimeString(applicationContext, service.duration() / 1000)
                val position = service.getPosition().toInt()
                elapsedText.value = SharedUtils.makeShortTimeString(applicationContext, position / 1000L)
                if (!userSeeking) {
                    progressMs.value = position
                }
                isPlaying.value = true
            }
            progressHandler.postDelayed(this, 50)
        }
    }

    private val updateLyrics = object : Runnable {
        override fun run() {
            val service = player_service
            if (service != null && service.isPlaying()) {
                val position = service.getPosition()
                val lyricsList = lyrics ?: emptyList()
                if (position > 0 && lyricsList.isNotEmpty()) {
                    for (i in lyricsList.indices) {
                        val pos = (lyricsList[i].getTime() * 1000).toInt()
                        if (pos <= position && currentLineIndex == 0) {
                            if (lyricsList[i].lyric != "") {
                                currentLyric.value = lyricsList[i].lyric.replace("&apos;", "'")
                            }
                            nextLyric.value = lyricsList.getOrNull(i + 1)?.lyric?.replace("&apos;", "'").orEmpty()
                        }
                        if (pos <= position && i > currentLineIndex) {
                            if (lyricsList[i].lyric != "") {
                                currentLyric.value = lyricsList[i].lyric.replace("&apos;", "'")
                            }
                            currentLineIndex = i
                            nextLyric.value = lyricsList.getOrNull(i + 1)?.lyric?.replace("&apos;", "'").orEmpty()
                        }
                    }
                }
            }
            lyricsHandler.postDelayed(this, 50)
        }
    }

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
        if (songIndex in list.indices) {
            getDetail(list[songIndex].id)
            getLyrics(list[songIndex].id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainHandler = Handler()
        progressHandler = Handler()
        lyricsHandler = Handler()

        if (SharedUtils.isMyServiceRunning(this, MediaPlayerService::class.java)) {
            val playerIntent = Intent(this, MediaPlayerService::class.java)
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        if (audioList.isEmpty() && MediaPlayerService.audioList.isNotEmpty()) {
            audioList = MediaPlayerService.audioList
        }

        val extras = intent.extras
        if (extras != null) {
            songIndex = extras.getInt("SONG_INDEX", MediaPlayerService.audioIndex)
            playAudio()
            MediaPlayerService.audioList = audioList
            MediaPlayerService.audioIndex = songIndex
            mainHandler.post(loadInfo)
        } else {
            songIndex = MediaPlayerService.audioIndex
            song = MediaPlayerService.song
            setDetails()
            if (audioList.isNotEmpty()) {
                getLyrics(audioList[songIndex].id)
            }
        }

        init()

        setContent {
            CloudMusicTheme {
                NowPlayingScreen(
                    songTitle = songTitle.value,
                    songArtist = songArtist.value,
                    coverUrl = coverUrl.value,
                    isPlaying = isPlaying.value,
                    shuffleEnabled = shuffleEnabled.value,
                    repeatMode = repeatMode.value,
                    progressMs = progressMs.value,
                    durationMs = durationMs.value,
                    elapsedText = elapsedText.value,
                    durationText = durationText.value,
                    currentLyric = currentLyric.value,
                    nextLyric = nextLyric.value,
                    isLiked = isLiked.value,
                    onBack = { finish() },
                    onPlayPause = { playOrPause() },
                    onPrevious = { skipToPrevious() },
                    onNext = { skipToNext() },
                    onShuffle = { setShuffle() },
                    onRepeat = { setRepeatMode() },
                    onToggleLike = { toggleLike() },
                    onSeekChange = { value ->
                        userSeeking = true
                        progressMs.value = value
                    },
                    onSeekEnd = {
                        userSeeking = false
                        player_service?.seek(progressMs.value)
                    },
                )
            }
        }
    }

    private fun init() {
        shuffleEnabled.value = SharedUtils.getShuffle(this)
        repeatMode.value = SharedUtils.getRepeatMode(this)
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
        if (lyrics != null) {
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

    private fun setIcon(event: IsPlayingEvent) {
        isPlaying.value = event.isPlaying
    }

    private fun setInfo(event: OnSourceChangeEvent) {
        Log.d("Event", event.message)
        resetLyricsView()
        songIndex = player_service?.current_index() ?: songIndex
        mainHandler.post(loadInfo)
    }

    private fun resetLyricsView() {
        currentLineIndex = 0
        currentLyric.value = ""
        nextLyric.value = ""
    }

    private fun getDetail(id: String?) {
        song = audioList.getOrNull(songIndex)
        if (song != null) {
            setDetails()
        }
    }

    private fun getLyrics(id: String?) {
        song = audioList.getOrNull(songIndex)
        val currentSong = song ?: return
        if (!TextUtils.isEmpty(currentSong.local_lyric)) {
            val lyricModel = LyricModel(1, 1, currentSong.local_lyric, 200)
            lyricModel.lyric?.let { lrc ->
                lyrics = Lyrics.parse(lrc)
            }
            if (!lyrics.isNullOrEmpty()) {
                lyricsHandler.postDelayed(updateLyrics, 10)
            } else {
                currentLyric.value = "No lyrics found"
                nextLyric.value = ""
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
                    if (!lyric?.lyric.isNullOrEmpty()) {
                        lyric.lyric?.let { lrc ->
                            lyrics = Lyrics.parse(lrc)
                        }
                        lyricsHandler.postDelayed(updateLyrics, 10)
                    } else {
                        currentLyric.value = "No lyrics found"
                        nextLyric.value = ""
                    }
                }.onFailure {
                    Log.e("App", "Fail to get lyrics")
                }
            }
        }
    }

    private fun setDetails() {
        val currentSong = audioList.getOrNull(songIndex) ?: return
        song = currentSong
        val picUrl = if (!TextUtils.isEmpty(currentSong.local_thumbnail)) {
            currentSong.local_thumbnail
        } else {
            SharedUtils.server + "pic/" + currentSong.pic_id
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                libraryRepository.addRecent(currentSong)
            }
        }
        lifecycleScope.launch {
            val liked = withContext(Dispatchers.IO) {
                libraryRepository.isLiked(currentSong.id)
            }
            isLiked.value = liked
        }
        songTitle.value = currentSong.name
        songArtist.value = TextUtils.join(", ", currentSong.artist)
        coverUrl.value = picUrl.orEmpty()
        val service = player_service
        if (service != null && service.isPlaying()) {
            durationMs.value = service.duration().toInt()
            durationText.value = SharedUtils.makeShortTimeString(applicationContext, service.duration() / 1000)
        }
        progressHandler.removeCallbacks(updateProgress)
        progressHandler.postDelayed(updateProgress, 10)
    }

    private fun skipToPrevious() {
        mainHandler.postDelayed({
            player_service?.skipToPrevious()
            songIndex = player_service?.current_index() ?: songIndex
            mainHandler.postDelayed(loadInfo, 10)
        }, 200)
    }

    private fun skipToNext() {
        mainHandler.postDelayed({
            player_service?.skipToNext()
            songIndex = player_service?.current_index() ?: songIndex
            mainHandler.postDelayed(loadInfo, 10)
        }, 200)
    }

    private fun setShuffle() {
        val next = !SharedUtils.getShuffle(this)
        SharedUtils.setShuffle(this, next)
        shuffleEnabled.value = next
    }

    private fun setRepeatMode() {
        when (SharedUtils.getRepeatMode(this)) {
            PlaybackMode.NORMAL -> {
                SharedUtils.setRepeatMode(this, PlaybackMode.REPEAT)
                player_service?.setLoop(false)
            }
            PlaybackMode.REPEAT -> {
                SharedUtils.setRepeatMode(this, PlaybackMode.REPEAT_ONE)
                player_service?.setLoop(true)
            }
            PlaybackMode.REPEAT_ONE -> {
                SharedUtils.setRepeatMode(this, PlaybackMode.NORMAL)
                player_service?.setLoop(false)
            }
        }
        repeatMode.value = SharedUtils.getRepeatMode(this)
    }

    private fun toggleLike() {
        val currentSong = song ?: return
        lifecycleScope.launch {
            val liked = withContext(Dispatchers.IO) {
                libraryRepository.toggleLiked(currentSong)
            }
            isLiked.value = liked
        }
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

    companion object {
        const val Broadcast_PLAY_NEW_AUDIO = "com.jgm90.cloudmusic.PlayNewAudio"

        @JvmField
        var audioList: MutableList<SongModel> = mutableListOf()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingScreen(
    songTitle: String,
    songArtist: String,
    coverUrl: String,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: PlaybackMode,
    progressMs: Int,
    durationMs: Int,
    elapsedText: String,
    durationText: String,
    currentLyric: String,
    nextLyric: String,
    isLiked: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onSeekChange: (Int) -> Unit,
    onSeekEnd: () -> Unit,
) {
    val sliderMax = if (durationMs > 0) durationMs else 1
    val backgroundGradient = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF0E1118),
                Color(0xFF1C2C3C),
                Color(0xFF0D3C3A)
            )
        )
    }
    val softGlow = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF5EEAD4).copy(alpha = 0.35f),
                Color.Transparent
            )
        )
    }
    val accentGradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    val infiniteTransition = rememberInfiniteTransition(label = "coverSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000),
            repeatMode = RepeatMode.Restart
        ),
        label = "coverSpinValue"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-90).dp, y = 40.dp)
                .background(softGlow, CircleShape)
                .alpha(0.6f)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 200.dp, y = 480.dp)
                .background(softGlow, CircleShape)
                .alpha(0.5f)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                            CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 2.sp,
                )
                Box(modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                            CircleShape
                        )
                )
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(2.dp, accentGradient, RoundedCornerShape(28.dp))
                        .shadow(24.dp, RoundedCornerShape(28.dp))
                        .graphicsLayer {
                            rotationZ = if (isPlaying) rotation else 0f
                        },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = songTitle.ifEmpty { "Reproduciendo" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = songArtist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Slider(
                value = progressMs.coerceIn(0, sliderMax).toFloat(),
                onValueChange = { onSeekChange(it.toInt()) },
                onValueChangeFinished = onSeekEnd,
                valueRange = 0f..sliderMax.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = elapsedText, style = MaterialTheme.typography.labelSmall, color = Color.White)
                Text(text = durationText, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = onShuffle, modifier = Modifier.alpha(if (shuffleEnabled) 1f else 0.4f)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shuffle_black_24dp),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                            CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentGradient, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                            CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onRepeat) {
                    val iconRes = when (repeatMode) {
                        PlaybackMode.NORMAL, PlaybackMode.REPEAT -> R.drawable.ic_repeat_black_24dp
                        PlaybackMode.REPEAT_ONE -> R.drawable.ic_repeat_one_black_24dp
                    }
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (currentLyric.isNotEmpty()) currentLyric else "No lyrics found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                )
                if (nextLyric.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nextLyric,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
