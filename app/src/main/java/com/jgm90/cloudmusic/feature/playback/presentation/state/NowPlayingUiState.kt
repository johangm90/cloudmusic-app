package com.jgm90.cloudmusic.feature.playback.presentation.state

import androidx.compose.runtime.Immutable
import com.jgm90.cloudmusic.core.playback.PlaybackMode
import com.jgm90.cloudmusic.feature.settings.domain.model.AppSettings

@Immutable
data class NowPlayingUiState(
    val songTitle: String = "",
    val songArtist: String = "",
    val coverUrl: String = "",
    val isPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: PlaybackMode = PlaybackMode.NORMAL,
    val progressMs: Int = 0,
    val durationMs: Int = 0,
    val elapsedText: String = "0:00",
    val durationText: String = "0:00",
    val currentLyric: String = "",
    val nextLyric: String = "",
    val isLiked: Boolean = false,
    val beatLevel: Float = 0f,
    val visualizerBands: FloatArray = FloatArray(0),
    val hasAudioPermission: Boolean = false,
    val settings: AppSettings = AppSettings(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NowPlayingUiState

        if (songTitle != other.songTitle) return false
        if (songArtist != other.songArtist) return false
        if (coverUrl != other.coverUrl) return false
        if (isPlaying != other.isPlaying) return false
        if (shuffleEnabled != other.shuffleEnabled) return false
        if (repeatMode != other.repeatMode) return false
        if (progressMs != other.progressMs) return false
        if (durationMs != other.durationMs) return false
        if (elapsedText != other.elapsedText) return false
        if (durationText != other.durationText) return false
        if (currentLyric != other.currentLyric) return false
        if (nextLyric != other.nextLyric) return false
        if (isLiked != other.isLiked) return false
        if (beatLevel != other.beatLevel) return false
        if (!visualizerBands.contentEquals(other.visualizerBands)) return false
        if (hasAudioPermission != other.hasAudioPermission) return false
        if (settings != other.settings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = songTitle.hashCode()
        result = 31 * result + songArtist.hashCode()
        result = 31 * result + coverUrl.hashCode()
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + shuffleEnabled.hashCode()
        result = 31 * result + repeatMode.hashCode()
        result = 31 * result + progressMs
        result = 31 * result + durationMs
        result = 31 * result + elapsedText.hashCode()
        result = 31 * result + durationText.hashCode()
        result = 31 * result + currentLyric.hashCode()
        result = 31 * result + nextLyric.hashCode()
        result = 31 * result + isLiked.hashCode()
        result = 31 * result + beatLevel.hashCode()
        result = 31 * result + visualizerBands.contentHashCode()
        result = 31 * result + hasAudioPermission.hashCode()
        result = 31 * result + settings.hashCode()
        return result
    }
}
