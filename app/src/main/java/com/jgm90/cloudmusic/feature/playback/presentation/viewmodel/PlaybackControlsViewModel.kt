package com.jgm90.cloudmusic.feature.playback.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.jgm90.cloudmusic.core.playback.PlaybackCommandController
import com.jgm90.cloudmusic.core.playback.PlaybackEventController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaybackControlsViewModel @Inject constructor(
    private val playbackEventController: PlaybackEventController,
    private val playbackCommandController: PlaybackCommandController,
) : ViewModel() {
    val info = playbackEventController.info
    val isLoading = playbackEventController.isLoading
    val isPlaying = playbackEventController.isPlaying

    fun togglePlayPause() {
        playbackCommandController.togglePlayPause()
    }
}
