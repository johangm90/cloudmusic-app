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
import coil3.request.allowHardware
import coil3.toBitmap
import com.jgm90.cloudmusic.core.data.local.repository.LibraryRepository
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.BeatEvent
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlaybackLoadingEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.core.event.VisualizerBandsEvent
import com.jgm90.cloudmusic.core.innertube.YouTubeRepository
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.core.playback.LyricLine
import com.jgm90.cloudmusic.core.playback.Lyrics
import com.jgm90.cloudmusic.core.playback.PlaybackMode
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.core.ui.theme.ThemeController
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
    private val youTubeRepository: YouTubeRepository,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private var lyrics: List<LyricLine>? = null
    private var currentLineIndex = 0
    private var userSeeking = false
    private var lastSeedUrl: String? = null
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
            },
            AppEventBus.observe<PlaybackLoadingEvent>(viewModelScope) { event ->
                _uiState.update { it.copy(isLoading = event.isLoading) }
            },
        )
    }

    fun stopEventObservation() {
        eventJobs.forEach { it.cancel() }
        eventJobs = emptyList()
    }

    fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var lastPosition = -1
            var lastDuration = -1
            var lastElapsedSec = -1
            var lastDurationSec = -1
            while (isActive) {
                val service = playerService
                if (service != null && service.isPlaying()) {
                    val duration = service.duration().toInt()
                    val position = service.getPosition().toInt()
                    val elapsedSec = (position / 1000L).toInt()
                    val durationSec = (duration / 1000L).toInt()
                    val context = getApplication<Application>()
                    val elapsedText = if (elapsedSec != lastElapsedSec) {
                        SharedUtils.makeShortTimeString(context, elapsedSec.toLong())
                    } else {
                        null
                    }
                    val durationText = if (durationSec != lastDurationSec) {
                        SharedUtils.makeShortTimeString(context, durationSec.toLong())
                    } else {
                        null
                    }
                    if (position != lastPosition || duration != lastDuration || !_uiState.value.isPlaying) {
                        _uiState.update {
                            it.copy(
                                durationMs = duration,
                                durationText = durationText ?: it.durationText,
                                progressMs = if (!userSeeking) position else it.progressMs,
                                elapsedText = elapsedText ?: it.elapsedText,
                                isPlaying = true
                            )
                        }
                    }
                    lastPosition = position
                    lastDuration = duration
                    lastElapsedSec = elapsedSec
                    lastDurationSec = durationSec
                } else if (_uiState.value.isPlaying) {
                    _uiState.update { it.copy(isPlaying = false) }
                }
                delay(200)
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
                val isPlaying = service?.isPlaying() == true
                if (isPlaying && !lyricsList.isNullOrEmpty()) {
                    val position = service?.getPosition() ?: 0L
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
                delay(if (isPlaying) 200 else 500)
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
        val picUrl = when {
            !song.local_thumbnail.isNullOrEmpty() -> song.local_thumbnail.orEmpty()
            song.isYouTubeSource() -> song.getCoverThumbnail()
            !song.pic_id.isNullOrEmpty() -> SharedUtils.server + "pic/" + song.pic_id
            else -> ""
        }

        _uiState.update {
            it.copy(
                songTitle = song.name,
                songArtist = TextUtils.join(", ", song.artist),
                coverUrl = picUrl
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

        val coverUrl = picUrl
        if (coverUrl.isNotEmpty() && coverUrl != lastSeedUrl) {
            lastSeedUrl = coverUrl
            updateThemeSeed(coverUrl)
        } else if (coverUrl.isEmpty()) {
            lastSeedUrl = null
            ThemeController.setSeedColor(null)
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
                val lyricText = runCatching {
                    if (song.isYouTubeSource()) {
                        val lyricId = song.lyric_id ?: song.id
                        if (lyricId.isNullOrBlank()) {
                            null
                        } else {
                            youTubeRepository.getLyrics(lyricId)?.lyric
                        }
                    } else {
                        withContext(Dispatchers.IO) {
                            restInterface.getLyrics(song.id)?.lyric
                        }
                    }
                }.getOrNull()

                if (!lyricText.isNullOrEmpty()) {
                    lyrics = Lyrics.parse(lyricText)
                    startLyricsSync()
                } else {
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

    private fun updateThemeSeed(url: String) {
        val cover = url.trim()
        if (cover.isEmpty()) {
            ThemeController.setSeedColor(null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(cover)
                .allowHardware(false)
                .build()
            val result = loader.execute(request) as? SuccessResult ?: return@launch
            val bitmap = result.image.toBitmap().let { source ->
                if (source.config == Bitmap.Config.HARDWARE) {
                    source.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    source
                }
            }
            val fallback = Color(0xFF0B1118).toArgb()
            val dominant = Palette.from(bitmap).generate().getDominantColor(fallback)
            withContext(Dispatchers.Main) {
                ThemeController.setSeedColor(Color(dominant))
            }
        }
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
