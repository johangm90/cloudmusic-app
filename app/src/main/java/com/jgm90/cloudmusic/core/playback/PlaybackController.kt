package com.jgm90.cloudmusic.core.playback

import com.jgm90.cloudmusic.core.model.SongModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackQueueState(
    val queue: List<SongModel> = emptyList(),
    val index: Int = -1,
) {
    val current: SongModel?
        get() = queue.getOrNull(index)
}

@Singleton
class PlaybackController @Inject constructor() {
    private val _queueState = MutableStateFlow(PlaybackQueueState())
    val queueState: StateFlow<PlaybackQueueState> = _queueState.asStateFlow()

    fun setQueue(queue: List<SongModel>, startIndex: Int = 0) {
        val normalizedIndex = if (queue.isEmpty()) {
            -1
        } else {
            startIndex.coerceIn(queue.indices)
        }
        _queueState.value = PlaybackQueueState(queue = queue.toList(), index = normalizedIndex)
    }

    fun setIndex(index: Int) {
        _queueState.update { state ->
            if (state.queue.isEmpty()) {
                state.copy(index = -1)
            } else {
                state.copy(index = index.coerceIn(state.queue.indices))
            }
        }
    }

    fun clearQueue() {
        _queueState.value = PlaybackQueueState()
    }
}
