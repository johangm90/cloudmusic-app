package com.jgm90.cloudmusic.feature.playback.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.text.TextUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.jgm90.cloudmusic.core.data.local.repository.LibraryRepository
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.BeatEvent
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.core.event.VisualizerBandsEvent
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.core.playback.LyricLine
import com.jgm90.cloudmusic.core.playback.Lyrics
import com.jgm90.cloudmusic.core.playback.PlaybackMode
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.feature.playback.presentation.state.AmbientColors
import com.jgm90.cloudmusic.feature.playback.presentation.state.NowPlayingAction
import com.jgm90.cloudmusic.feature.playback.presentation.state.NowPlayingUiState
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import com.jgm90.cloudmusic.feature.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    application: Application,
    private val restInterface: RestInterface,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private var lyrics: List<LyricLine>? = null
    private var currentLineIndex = 0
    private var userSeeking = false
    private var lastPaletteUrl: String? = null
    private var progressJob: Job? = null
    private var lyricsJob: Job? = null
    private var eventJobs: List<Job> = emptyList()
    private var settingsJob: Job? = null

    var playerService: MediaPlayerService? = null
        set(value) {
            field = value
            if (value != null) {
                startProgressUpdates()
            }
        }

    init {
        _uiState.update {
            it.copy(
                shuffleEnabled = SharedUtils.getShuffle(application),
                repeatMode = SharedUtils.getRepeatMode(application),
                settings = settingsRepository.settings.value
            )
        }
        observeSettings()
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun startEventObservation() {
        stopEventObservation()
        eventJobs = listOf(
            AppEventBus.observe<IsPlayingEvent>(viewModelScope) { event ->
                _uiState.update { it.copy(isPlaying = event.isPlaying) }
            },
            AppEventBus.observe<OnSourceChangeEvent>(viewModelScope) { event ->
                resetLyrics()
                loadCurrentSongInfo()
            },
            AppEventBus.observe<BeatEvent>(viewModelScope, receiveSticky = false) { event ->
                _uiState.update { it.copy(beatLevel = event.level) }
            },
            AppEventBus.observe<VisualizerBandsEvent>(viewModelScope, receiveSticky = false) { event ->
                _uiState.update { it.copy(visualizerBands = event.bands) }
            }
        )
    }

    fun stopEventObservation() {
        eventJobs.forEach { it.cancel() }
        eventJobs = emptyList()
    }

    fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val service = playerService
                if (service != null && service.isPlaying()) {
                    val duration = service.duration().toInt()
                    val position = service.getPosition().toInt()
                    val context = getApplication<Application>()
                    _uiState.update {
                        it.copy(
                            durationMs = duration,
                            durationText = SharedUtils.makeShortTimeString(context, duration / 1000L),
                            progressMs = if (!userSeeking) position else it.progressMs,
                            elapsedText = SharedUtils.makeShortTimeString(context, position / 1000L),
                            isPlaying = true
                        )
                    }
                }
                delay(50)
            }
        }
    }

    fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun startLyricsSync() {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            while (isActive) {
                val service = playerService
                val lyricsList = lyrics
                if (service != null && service.isPlaying() && !lyricsList.isNullOrEmpty()) {
                    val position = service.getPosition()
                    if (position > 0) {
                        val index = findLyricIndex(lyricsList, position)
                        if (index >= 0 && index != currentLineIndex) {
                            currentLineIndex = index
                            val currentLyric = lyricsList[index].lyric
                                .replace("&apos;", "'")
                                .takeIf { it.isNotEmpty() }
                            val nextLyric = lyricsList.getOrNull(index + 1)?.lyric
                                ?.replace("&apos;", "'")
                                .orEmpty()
                            _uiState.update {
                                it.copy(
                                    currentLyric = currentLyric ?: it.currentLyric,
                                    nextLyric = nextLyric
                                )
                            }
                        }
                    }
                }
                delay(50)
            }
        }
    }

    private fun findLyricIndex(lyricsList: List<LyricLine>, positionMs: Long): Int {
        var low = 0
        var high = lyricsList.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) / 2
            val lyricTime = (lyricsList[mid].getTime() * 1000).toLong()
            if (lyricTime <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    fun loadSongInfo(songIndex: Int, audioList: List<SongModel>) {
        if (songIndex !in audioList.indices) return
        val song = audioList[songIndex]
        loadSongDetails(song)
        loadLyrics(song)
    }

    fun loadCurrentSongInfo() {
        val service = playerService ?: return
        val index = service.current_index()
        val audioList = MediaPlayerService.audioList
        if (index in audioList.indices) {
            loadSongInfo(index, audioList)
        }
    }

    private fun loadSongDetails(song: SongModel) {
        val context = getApplication<Application>()
        val picUrl = if (!TextUtils.isEmpty(song.local_thumbnail)) {
            song.local_thumbnail
        } else {
            SharedUtils.server + "pic/" + song.pic_id
        }

        _uiState.update {
            it.copy(
                songTitle = song.name,
                songArtist = TextUtils.join(", ", song.artist),
                coverUrl = picUrl.orEmpty()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            libraryRepository.addRecent(song)
        }

        viewModelScope.launch {
            val liked = withContext(Dispatchers.IO) {
                libraryRepository.isLiked(song.id)
            }
            _uiState.update { it.copy(isLiked = liked) }
        }

        val coverUrl = picUrl.orEmpty()
        if (coverUrl.isNotEmpty() && coverUrl != lastPaletteUrl) {
            lastPaletteUrl = coverUrl
            updatePaletteFromCover(coverUrl)
        }

        val service = playerService
        if (service != null && service.isPlaying()) {
            val duration = service.duration().toInt()
            _uiState.update {
                it.copy(
                    durationMs = duration,
                    durationText = SharedUtils.makeShortTimeString(context, duration / 1000L)
                )
            }
        }
    }

    private fun loadLyrics(song: SongModel) {
        val context = getApplication<Application>()
        if (!TextUtils.isEmpty(song.local_lyric)) {
            song.local_lyric?.let { lrc ->
                lyrics = Lyrics.parse(lrc)
                if (!lyrics.isNullOrEmpty()) {
                    startLyricsSync()
                } else {
                    _uiState.update { it.copy(currentLyric = "No lyrics found", nextLyric = "") }
                }
            }
        } else {
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        restInterface.getLyrics(song.id)
                    }
                }.onSuccess { lyric ->
                    if (!lyric?.lyric.isNullOrEmpty()) {
                        lyric.lyric?.let { lrc ->
                            lyrics = Lyrics.parse(lrc)
                            startLyricsSync()
                        }
                    } else {
                        _uiState.update { it.copy(currentLyric = "No lyrics found", nextLyric = "") }
                    }
                }.onFailure {
                    _uiState.update { it.copy(currentLyric = "No lyrics found", nextLyric = "") }
                }
            }
        }
    }

    private fun resetLyrics() {
        currentLineIndex = 0
        lyrics = null
        _uiState.update { it.copy(currentLyric = "", nextLyric = "") }
    }

    private fun updatePaletteFromCover(url: String) {
        val cover = url.trim()
        if (cover.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(cover)
                .build()
            val result = loader.execute(request)
            val image = (result as? SuccessResult)?.image ?: return@launch
            val bitmap = image.toBitmap().let { source ->
                if (source.config == Bitmap.Config.HARDWARE) {
                    source.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    source
                }
            }
            val palette = Palette.from(bitmap).generate()
            val fallback = _uiState.value.ambientColors.accentPrimary.toArgb()
            val dominant = palette.getDominantColor(fallback)
            val vibrant = palette.getVibrantColor(dominant)
            val muted = palette.getMutedColor(dominant)
            val darkVibrant = palette.getDarkVibrantColor(dominant)
            val darkMuted = palette.getDarkMutedColor(dominant)
            val baseDark = Color(0xFF0B1118)
            val bgStart = blend(baseDark, Color(darkMuted), 0.35f)
            val bgMid = blend(baseDark, Color(muted), 0.45f)
            val bgEnd = blend(baseDark, Color(darkVibrant), 0.35f)
            val accentA = Color(vibrant)
            val accentB = Color(dominant)
            val paletteList = listOf(
                accentA,
                Color(muted),
                Color(vibrant),
                Color(dominant),
                Color(darkVibrant),
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        ambientColors = AmbientColors(
                            backgroundStart = bgStart,
                            backgroundMid = bgMid,
                            backgroundEnd = bgEnd,
                            accentPrimary = accentA,
                            accentSecondary = accentB,
                            particleColors = paletteList,
                        ),
                        paletteReady = true
                    )
                }
            }
        }
    }

    private fun blend(base: Color, overlay: Color, ratio: Float): Color {
        val t = ratio.coerceIn(0f, 1f)
        val r = base.red * (1f - t) + overlay.red * t
        val g = base.green * (1f - t) + overlay.green * t
        val b = base.blue * (1f - t) + overlay.blue * t
        return Color(r, g, b, 1f)
    }

    fun onAction(action: NowPlayingAction) {
        when (action) {
            is NowPlayingAction.PlayPause -> {
                viewModelScope.launch {
                    delay(250)
                    playerService?.eventPlayOrPause(PlayPauseEvent("From ViewModel"))
                }
            }
            is NowPlayingAction.SkipToPrevious -> {
                viewModelScope.launch {
                    delay(200)
                    playerService?.skipToPrevious()
                    delay(10)
                    loadCurrentSongInfo()
                }
            }
            is NowPlayingAction.SkipToNext -> {
                viewModelScope.launch {
                    delay(200)
                    playerService?.skipToNext()
                    delay(10)
                    loadCurrentSongInfo()
                }
            }
            is NowPlayingAction.ToggleShuffle -> {
                val context = getApplication<Application>()
                val next = !SharedUtils.getShuffle(context)
                SharedUtils.setShuffle(context, next)
                _uiState.update { it.copy(shuffleEnabled = next) }
            }
            is NowPlayingAction.ToggleRepeat -> {
                val context = getApplication<Application>()
                when (SharedUtils.getRepeatMode(context)) {
                    PlaybackMode.NORMAL -> {
                        SharedUtils.setRepeatMode(context, PlaybackMode.REPEAT)
                        playerService?.setLoop(false)
                    }
                    PlaybackMode.REPEAT -> {
                        SharedUtils.setRepeatMode(context, PlaybackMode.REPEAT_ONE)
                        playerService?.setLoop(true)
                    }
                    PlaybackMode.REPEAT_ONE -> {
                        SharedUtils.setRepeatMode(context, PlaybackMode.NORMAL)
                        playerService?.setLoop(false)
                    }
                }
                _uiState.update { it.copy(repeatMode = SharedUtils.getRepeatMode(context)) }
            }
            is NowPlayingAction.ToggleLike -> {
                val service = playerService ?: return
                val index = service.current_index()
                val audioList = MediaPlayerService.audioList
                if (index !in audioList.indices) return
                val song = audioList[index]
                viewModelScope.launch {
                    val liked = withContext(Dispatchers.IO) {
                        libraryRepository.toggleLiked(song)
                    }
                    _uiState.update { it.copy(isLiked = liked) }
                }
            }
            is NowPlayingAction.SeekTo -> {
                playerService?.seek(action.positionMs)
            }
            is NowPlayingAction.UpdateProgress -> {
                _uiState.update { it.copy(progressMs = action.positionMs) }
            }
            is NowPlayingAction.StartSeeking -> {
                userSeeking = true
            }
            is NowPlayingAction.StopSeeking -> {
                userSeeking = false
                playerService?.seek(_uiState.value.progressMs)
            }
        }
    }

    fun updateAudioPermission(hasPermission: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = hasPermission) }
    }

    override fun onCleared() {
        super.onCleared()
        stopEventObservation()
        stopProgressUpdates()
        lyricsJob?.cancel()
        settingsJob?.cancel()
    }
}
