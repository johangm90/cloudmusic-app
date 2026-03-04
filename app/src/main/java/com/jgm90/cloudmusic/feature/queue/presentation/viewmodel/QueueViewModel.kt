package com.jgm90.cloudmusic.feature.queue.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.jgm90.cloudmusic.core.playback.PlaybackController
import com.jgm90.cloudmusic.core.playback.PlaybackQueueState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playbackController: PlaybackController,
) : ViewModel() {

    val queueState: StateFlow<PlaybackQueueState> = playbackController.queueState

    fun select(index: Int) {
        playbackController.setIndex(index)
    }

    fun remove(index: Int) {
        playbackController.removeAt(index)
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        playbackController.move(index, index - 1)
    }

    fun moveDown(index: Int) {
        val max = queueState.value.queue.lastIndex
        if (index < 0 || index >= max) return
        playbackController.move(index, index + 1)
    }
}
