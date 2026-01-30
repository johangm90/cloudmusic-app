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
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.playback.LyricLine
import com.jgm90.cloudmusic.core.playback.PlaybackMode
import com.jgm90.cloudmusic.core.playback.PlaybackEventController
import com.jgm90.cloudmusic.core.playback.PlaybackController
import com.jgm90.cloudmusic.core.playback.PlaybackPreferences
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.core.ui.theme.ThemeController
import com.jgm90.cloudmusic.feature.playback.domain.usecase.GetLyricsUseCase
import com.jgm90.cloudmusic.feature.playback.domain.usecase.PlaybackLibraryUseCase
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
    private val settingsRepository: SettingsRepository,
    private val playbackController: PlaybackController,
    private val getLyricsUseCase: GetLyricsUseCase,
    private val playbackLibraryUseCase: PlaybackLibraryUseCase,
    private val playbackPreferences: PlaybackPreferences,
    private val imageLoader: ImageLoader,
    private val playbackEventController: PlaybackEventController,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private var lyrics: List<LyricLine>? = null
    private var currentLineIndex = 0
    private var userSeeking = false
    private var lastSeedUrl: String? = null
    private var lastPosition = -1
    private var lastDuration = -1
    private var lastElapsedSec = -1
    private var lastDurationSec = -1
    private var lyricsJob: Job? = null
    private var eventJobs: List<Job> = emptyList()
    private var settingsJob: Job? = null

    var playerService: MediaPlayerService? = null
        set(value) {
            field = value
        }

    init {
        _uiState.update {
            it.copy(
                shuffleEnabled = playbackPreferences.shuffleEnabled.value,
                repeatMode = playbackPreferences.repeatMode.value,
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
            viewModelScope.launch {
                playbackEventController.isPlaying.collect { isPlaying ->
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }
            },
            viewModelScope.launch {
                playbackEventController.sourceChanged.collect {
                    resetLyrics()
                    loadCurrentSongInfo()
                }
            },
            viewModelScope.launch {
                playbackEventController.beat.collect { level ->
                    _uiState.update { it.copy(beatLevel = level) }
                }
            },
            viewModelScope.launch {
                playbackEventController.bands.collect { bands ->
                    _uiState.update { it.copy(visualizerBands = bands) }
                }
            },
            viewModelScope.launch {
                playbackEventController.isLoading.collect { isLoading ->
                    _uiState.update { it.copy(isLoading = isLoading) }
                }
            },
            viewModelScope.launch {
                playbackEventController.progress.collect { progress ->
                    val duration = progress.durationMs
                    val position = progress.positionMs
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
                    if (position != lastPosition || duration != lastDuration) {
                        _uiState.update {
                            it.copy(
                                durationMs = duration,
                                durationText = durationText ?: it.durationText,
                                progressMs = if (!userSeeking) position else it.progressMs,
                                elapsedText = elapsedText ?: it.elapsedText,
                            )
                        }
                    }
                    lastPosition = position
                    lastDuration = duration
                    lastElapsedSec = elapsedSec
                    lastDurationSec = durationSec
                }
            },
        )
    }

    fun stopEventObservation() {
        eventJobs.forEach { it.cancel() }
        eventJobs = emptyList()
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
                delay(if (playerService?.isPlaying() == true) 200 else 500)
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
        val audioList = playbackController.queueState.value.queue
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
            playbackLibraryUseCase.addRecent(song)
        }

        viewModelScope.launch {
            val liked = withContext(Dispatchers.IO) {
                playbackLibraryUseCase.isLiked(song.id)
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
        viewModelScope.launch {
            val lyricLines = runCatching {
                getLyricsUseCase.execute(song)
            }.getOrNull()

            if (!lyricLines.isNullOrEmpty()) {
                lyrics = lyricLines
                startLyricsSync()
            } else {
                _uiState.update { it.copy(currentLyric = "No lyrics found", nextLyric = "") }
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
            val request = ImageRequest.Builder(context)
                .data(cover)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request) as? SuccessResult ?: return@launch
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
                    playerService?.togglePlayPause()
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
                val next = !playbackPreferences.shuffleEnabled.value
                playbackPreferences.setShuffle(next)
                _uiState.update { it.copy(shuffleEnabled = next) }
            }
            is NowPlayingAction.ToggleRepeat -> {
                val current = playbackPreferences.repeatMode.value
                when (current) {
                    PlaybackMode.NORMAL -> {
                        playbackPreferences.setRepeatMode(PlaybackMode.REPEAT)
                        playerService?.setLoop(false)
                    }
                    PlaybackMode.REPEAT -> {
                        playbackPreferences.setRepeatMode(PlaybackMode.REPEAT_ONE)
                        playerService?.setLoop(true)
                    }
                    PlaybackMode.REPEAT_ONE -> {
                        playbackPreferences.setRepeatMode(PlaybackMode.NORMAL)
                        playerService?.setLoop(false)
                    }
                }
                _uiState.update { it.copy(repeatMode = playbackPreferences.repeatMode.value) }
            }
            is NowPlayingAction.ToggleLike -> {
                val service = playerService ?: return
                val index = service.current_index()
                val audioList = playbackController.queueState.value.queue
                if (index !in audioList.indices) return
                val song = audioList[index]
                viewModelScope.launch {
                    val liked = withContext(Dispatchers.IO) {
                        playbackLibraryUseCase.toggleLiked(song)
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
        lyricsJob?.cancel()
        settingsJob?.cancel()
    }
}
