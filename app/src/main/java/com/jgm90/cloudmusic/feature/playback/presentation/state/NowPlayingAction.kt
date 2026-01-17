package com.jgm90.cloudmusic.feature.playback.presentation.state

sealed interface NowPlayingAction {
    data object PlayPause : NowPlayingAction
    data object SkipToPrevious : NowPlayingAction
    data object SkipToNext : NowPlayingAction
    data object ToggleShuffle : NowPlayingAction
    data object ToggleRepeat : NowPlayingAction
    data object ToggleLike : NowPlayingAction
    data class SeekTo(val positionMs: Int) : NowPlayingAction
    data class UpdateProgress(val positionMs: Int) : NowPlayingAction
    data object StartSeeking : NowPlayingAction
    data object StopSeeking : NowPlayingAction
}
