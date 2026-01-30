package com.jgm90.cloudmusic.core.playback

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackInfo(
    val title: String = "",
    val artist: String = "",
    val artUrl: String = "",
    val isPlaying: Boolean = false,
)

data class PlaybackProgress(
    val positionMs: Int = 0,
    val durationMs: Int = 0,
)

@Singleton
class PlaybackEventController @Inject constructor() {
    private val _info = MutableStateFlow(PlaybackInfo())
    val info: StateFlow<PlaybackInfo> = _info.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress: StateFlow<PlaybackProgress> = _progress.asStateFlow()

    private val _sourceChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sourceChanged = _sourceChanged.asSharedFlow()

    private val _beat = MutableSharedFlow<Float>(extraBufferCapacity = 64)
    val beat = _beat.asSharedFlow()

    private val _bands = MutableSharedFlow<FloatArray>(extraBufferCapacity = 16)
    val bands = _bands.asSharedFlow()

    fun updateInfo(title: String, artist: String, artUrl: String, isPlaying: Boolean) {
        _info.value = PlaybackInfo(title = title, artist = artist, artUrl = artUrl, isPlaying = isPlaying)
        _isPlaying.value = isPlaying
    }

    fun setIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        _info.update { it.copy(isPlaying = isPlaying) }
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun updateProgress(positionMs: Int, durationMs: Int) {
        _progress.value = PlaybackProgress(positionMs = positionMs, durationMs = durationMs)
    }

    fun emitSourceChanged() {
        _sourceChanged.tryEmit(Unit)
    }

    fun emitBeat(level: Float) {
        _beat.tryEmit(level)
    }

    fun emitBands(bands: FloatArray) {
        _bands.tryEmit(bands)
    }
}
