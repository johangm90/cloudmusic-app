package com.jgm90.cloudmusic.feature.playback.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.data.local.repository.LibraryRepository
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.BeatEvent
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.core.event.VisualizerBandsEvent
import com.jgm90.cloudmusic.core.model.LyricModel
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.core.playback.LyricLine
import com.jgm90.cloudmusic.core.playback.Lyrics
import com.jgm90.cloudmusic.core.playback.PlaybackMode
import com.jgm90.cloudmusic.core.ui.theme.CloudMusicTheme
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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
    private val beatLevel = mutableStateOf(0f)
    private val hasAudioPermission = mutableStateOf(false)
    private val visualizerBands = mutableStateOf(FloatArray(0))

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
                durationText.value =
                    SharedUtils.makeShortTimeString(applicationContext, service.duration() / 1000)
                val position = service.getPosition().toInt()
                elapsedText.value =
                    SharedUtils.makeShortTimeString(applicationContext, position / 1000L)
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
                            nextLyric.value =
                                lyricsList.getOrNull(i + 1)?.lyric?.replace("&apos;", "'").orEmpty()
                        }
                        if (pos <= position && i > currentLineIndex) {
                            if (lyricsList[i].lyric != "") {
                                currentLyric.value = lyricsList[i].lyric.replace("&apos;", "'")
                            }
                            currentLineIndex = i
                            nextLyric.value =
                                lyricsList.getOrNull(i + 1)?.lyric?.replace("&apos;", "'").orEmpty()
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
        hasAudioPermission.value = hasAudioPermission()
        requestAudioPermission()

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
                    beatLevel = beatLevel.value,
                    hasAudioPermission = hasAudioPermission.value,
                    visualizerBands = visualizerBands.value,
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

    private fun requestAudioPermission() {
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (!hasAudioPermission.value) {
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
            hasAudioPermission.value = hasAudioPermission()
        }
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
            AppEventBus.observe<BeatEvent>(lifecycleScope, receiveSticky = false) { event ->
                beatLevel.value = event.level
            },
            AppEventBus.observe<VisualizerBandsEvent>(
                lifecycleScope,
                receiveSticky = false
            ) { event ->
                visualizerBands.value = event.bands
            },
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
            durationText.value =
                SharedUtils.makeShortTimeString(applicationContext, service.duration() / 1000)
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

@Composable
private fun BeatWaveRing(
    modifier: Modifier,
    beatLevel: Float,
    isPlaying: Boolean,
    bands: FloatArray,
) {
    val transition = rememberInfiniteTransition(label = "waveRing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.33f
        val amp = size.minDimension * (0.06f + (beatLevel.coerceIn(0f, 1f) * 0.18f))
        val waveIntensity = if (isPlaying) 1f else 0.15f
        val stroke = Stroke(
            width = size.minDimension * 0.012f,
            cap = StrokeCap.Round
        )
        val brush = Brush.sweepGradient(
            listOf(
                Color(0xFF5EEAD4),
                Color(0xFF60A5FA),
                Color(0xFFF59E0B),
                Color(0xFF5EEAD4)
            ),
            center = center
        )
        val steps = if (bands.isNotEmpty()) bands.size else 96
        val step = (2f * PI.toFloat()) / steps
        val glowStroke = Stroke(width = size.minDimension * 0.03f, cap = StrokeCap.Round)
        for (i in 0 until steps) {
            val angle = i * step
            val bandLevel = if (bands.isNotEmpty()) bands[i].coerceIn(0f, 1f) else
                ((sin(angle * 8f + phase) * 0.5f + 0.5f) * beatLevel).coerceIn(0f, 1f)
            val wave = sin(angle * 12f + phase) * amp * waveIntensity
            val spike = amp * (0.45f + bandLevel * 1.6f) * waveIntensity
            val r1 = baseRadius + wave
            val r2 = r1 + spike
            val x1 = center.x + cos(angle) * r1
            val y1 = center.y + sin(angle) * r1
            val x2 = center.x + cos(angle) * r2
            val y2 = center.y + sin(angle) * r2
            drawLine(
                brush = brush,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = glowStroke.width,
                cap = glowStroke.cap,
                alpha = 0.25f + bandLevel * 0.2f
            )
            drawLine(
                brush = brush,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = stroke.width,
                cap = stroke.cap
            )
        }
    }
}

@Composable
private fun FancySeekBar(
    progress: Int,
    max: Int,
    onSeekChange: (Int) -> Unit,
    onSeekEnd: () -> Unit,
    activeBrush: Brush,
    inactiveColor: Color,
    beatLevel: Float,
) {
    var size by remember { mutableStateOf(IntSize(1, 1)) }
    val fraction = if (max > 0) progress.toFloat() / max else 0f
    val clamped = fraction.coerceIn(0f, 1f)
    val thumbPulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbPulse"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .onSizeChanged { size = it }
            .pointerInput(max) {
                detectDragGestures(
                    onDragEnd = { onSeekEnd() },
                    onDragCancel = { onSeekEnd() },
                ) { change, _ ->
                    change.consume()
                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                    val newValue = ((x / size.width) * max).toInt()
                    onSeekChange(newValue)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = size.height * 0.18f
            val centerY = size.height / 2f
            val start = Offset(0f, centerY)
            val end = Offset(size.width.toFloat(), centerY)
            val progressX = size.width * clamped

            drawLine(
                color = inactiveColor,
                start = start,
                end = end,
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )
            drawLine(
                brush = activeBrush,
                start = start,
                end = Offset(progressX, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )

            val waveAmp = trackHeight * (1.2f + beatLevel.coerceIn(0f, 1f) * 2.2f)
            val waveSteps = 40
            val waveStep = size.width.toFloat() / waveSteps
            for (i in 0 until waveSteps) {
                val x1 = i * waveStep
                val x2 = (i + 1) * waveStep
                val t1 = x1 / size.width.toFloat()
                val t2 = x2 / size.width.toFloat()
                val y1 = centerY + sin((t1 * 10f + clamped * 4f) * PI.toFloat()) * waveAmp * 0.2f
                val y2 = centerY + sin((t2 * 10f + clamped * 4f) * PI.toFloat()) * waveAmp * 0.2f
                val alpha = if (x2 <= progressX) 0.35f else 0.15f
                drawLine(
                    brush = activeBrush,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = trackHeight * 0.35f,
                    cap = StrokeCap.Round,
                    alpha = alpha
                )
            }

            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = trackHeight * 2.2f,
                center = Offset(progressX, centerY),
            )
            drawCircle(
                brush = activeBrush,
                radius = trackHeight * 1.6f * thumbPulse,
                center = Offset(progressX, centerY),
            )
        }
    }
}

@Composable
private fun AmbientParticles(
    modifier: Modifier,
    intensity: Float,
    enabled: Boolean,
) {
    val particles = remember {
        List(18) {
            val angle = Random.nextFloat() * (2f * PI.toFloat())
            val radius = Random.nextFloat() * 0.45f + 0.15f
            val speed = Random.nextFloat() * 0.6f + 0.2f
            Particle(angle, radius, speed)
        }
    }
    val transition = rememberInfiniteTransition(label = "particles")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlesTime"
    )
    Canvas(modifier = modifier) {
        if (!enabled) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension
        for (p in particles) {
            val phase = time * p.speed
            val angle = p.angle + phase
            val r = base * p.radius * (1f + intensity * 0.2f)
            val x = center.x + cos(angle) * r
            val y = center.y + sin(angle) * r
            val alpha = 0.25f + intensity * 0.35f
            drawCircle(
                color = Color(0xFF5EEAD4).copy(alpha = alpha),
                radius = base * 0.008f,
                center = Offset(x, y),
            )
        }
    }
}

private data class Particle(
    val angle: Float,
    val radius: Float,
    val speed: Float,
)

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
    beatLevel: Float,
    hasAudioPermission: Boolean,
    visualizerBands: FloatArray,
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
    val useShader = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var bgSize by remember { mutableStateOf(IntSize(1, 1)) }
    var ringSize by remember { mutableStateOf(IntSize(1, 1)) }
    var shaderTime by remember { mutableStateOf(0f) }
    val backgroundGradient = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF0B0F14),
                Color(0xFF12202C),
                Color(0xFF0B2B2A)
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
    val simulatedBeat by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "simulatedBeat"
    )
    val effectiveBeat = if (hasAudioPermission) beatLevel else simulatedBeat
    val playingBeat = if (isPlaying) effectiveBeat else 0f
    val pulse by animateFloatAsState(
        targetValue = 1f + playingBeat.coerceIn(0f, 1f) * 0.18f,
        animationSpec = tween(120),
        label = "beatPulse"
    )
    val backgroundShader = remember {
        if (!useShader) null else RuntimeShader(
            """
            uniform float2 u_resolution;
            uniform float u_time;
            half4 main(float2 fragCoord) {
              float2 uv = fragCoord / u_resolution;
              float2 p = uv * 2.0 - 1.0;
              float v = sin((p.x + p.y + u_time) * 2.0) * 0.2;
              v += sin((p.x * 1.7 - p.y * 1.1 + u_time * 1.3) * 2.5) * 0.2;
              float glow = exp(-dot(p, p) * 1.6);
              float3 c1 = float3(0.04, 0.08, 0.11);
              float3 c2 = float3(0.09, 0.20, 0.26);
              float3 c3 = float3(0.06, 0.28, 0.25);
              float t = clamp(v * 0.5 + glow * 0.6, 0.0, 1.0);
              float3 col = mix(c1, c2, uv.y) + c3 * t;
              return half4(col, 1.0);
            }
            """.trimIndent()
        )
    }
    val ringShader = remember {
        if (!useShader) null else RuntimeShader(
            """
            uniform float2 u_resolution;
            uniform float u_time;
            uniform float u_amp;
            half4 main(float2 fragCoord) {
              float2 uv = (fragCoord / u_resolution) * 2.0 - 1.0;
              float r = length(uv);
              float angle = atan(uv.y, uv.x);
              float wave = sin(angle * 12.0 + u_time * 4.0) * (0.03 + u_amp * 0.08);
              float ring = smoothstep(0.82 + wave, 0.80 + wave, r) * smoothstep(0.68, 0.70, r);
              float glow = smoothstep(0.98, 0.75, r) * (0.25 + u_amp * 0.55);
              float mixv = (sin(u_time + angle) + 1.0) * 0.5;
              float3 color = mix(float3(0.2, 0.9, 0.8), float3(0.95, 0.7, 0.2), mixv);
              float alpha = ring * 0.95 + glow * 0.35;
              return half4(color * alpha, alpha);
            }
            """.trimIndent()
        )
    }
    val backgroundBrush = remember(backgroundShader) {
        backgroundShader?.let { ShaderBrush(it) }
    }
    val ringBrush = remember(ringShader) {
        ringShader?.let { ShaderBrush(it) }
    }
    LaunchedEffect(useShader) {
        if (!useShader) return@LaunchedEffect
        while (true) {
            withFrameNanos { frameTime ->
                shaderTime = frameTime / 1_000_000_000f
            }
        }
    }
    val shaderBackgroundReady =
        useShader && bgSize.width > 1 && bgSize.height > 1 && backgroundShader != null
    val shaderRingReady =
        useShader && ringSize.width > 1 && ringSize.height > 1 && ringShader != null
    if (shaderBackgroundReady) {
        backgroundShader?.setFloatUniform(
            "u_resolution",
            bgSize.width.toFloat(),
            bgSize.height.toFloat()
        )
        backgroundShader?.setFloatUniform("u_time", shaderTime)
    }
    if (shaderRingReady) {
        ringShader?.setFloatUniform(
            "u_resolution",
            ringSize.width.toFloat(),
            ringSize.height.toFloat()
        )
        ringShader?.setFloatUniform("u_time", shaderTime)
        ringShader?.setFloatUniform("u_amp", playingBeat.coerceIn(0f, 1f))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { bgSize = it }
            .background(
                if (shaderBackgroundReady) backgroundBrush!! else backgroundGradient
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1118).copy(alpha = 0.35f))
        )
        AmbientParticles(
            modifier = Modifier.fillMaxSize(),
            intensity = playingBeat,
            enabled = isPlaying,
        )
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
                BeatWaveRing(
                    modifier = Modifier.size(320.dp),
                    beatLevel = playingBeat,
                    isPlaying = isPlaying,
                    bands = visualizerBands,
                )
                Box(
                    modifier = Modifier
                        .size(288.dp)
                        .clip(CircleShape)
                        .onSizeChanged { ringSize = it }
                        .background(if (shaderRingReady) ringBrush!! else accentGradient)
                        .graphicsLayer {
                            rotationZ = if (isPlaying) rotation else 0f
                            scaleX = pulse
                            scaleY = pulse
                            alpha = 0.75f + (playingBeat.coerceIn(0f, 1f) * 0.2f)
                        }
                )
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(260.dp)
                        .clip(CircleShape)
                        .border(
                            width = (2.dp + (playingBeat * 5f).dp),
                            brush = accentGradient,
                            shape = CircleShape
                        )
                        .shadow(24.dp, CircleShape)
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
            FancySeekBar(
                progress = progressMs.coerceIn(0, sliderMax),
                max = sliderMax,
                onSeekChange = onSeekChange,
                onSeekEnd = onSeekEnd,
                activeBrush = accentGradient,
                inactiveColor = Color.White.copy(alpha = 0.2f),
                beatLevel = playingBeat,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = elapsedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
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
                IconButton(
                    onClick = onShuffle,
                    modifier = Modifier.alpha(if (shuffleEnabled) 1f else 0.4f)
                ) {
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
                    text = currentLyric.ifEmpty { "No lyrics found" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                if (nextLyric.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nextLyric,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
