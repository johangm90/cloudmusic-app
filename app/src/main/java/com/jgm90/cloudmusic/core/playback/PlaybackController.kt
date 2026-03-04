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


    fun appendToQueue(song: SongModel) {
        _queueState.update { state ->
            if (state.queue.isEmpty()) {
                PlaybackQueueState(queue = listOf(song), index = 0)
            } else {
                state.copy(queue = state.queue + song)
            }
        }
    }

    fun insertNext(song: SongModel) {
        _queueState.update { state ->
            if (state.queue.isEmpty()) {
                PlaybackQueueState(queue = listOf(song), index = 0)
            } else {
                val insertIndex = (state.index + 1).coerceAtMost(state.queue.size)
                val nextQueue = state.queue.toMutableList().apply {
                    add(insertIndex, song)
                }
                state.copy(queue = nextQueue)
            }
        }
    }


    fun removeAt(index: Int) {
        _queueState.update { state ->
            if (index !in state.queue.indices) {
                return@update state
            }
            val nextQueue = state.queue.toMutableList().apply { removeAt(index) }
            if (nextQueue.isEmpty()) {
                return@update PlaybackQueueState()
            }
            val nextIndex = when {
                index < state.index -> state.index - 1
                index == state.index -> state.index.coerceAtMost(nextQueue.lastIndex)
                else -> state.index
            }.coerceIn(nextQueue.indices)
            PlaybackQueueState(queue = nextQueue, index = nextIndex)
        }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        _queueState.update { state ->
            if (fromIndex !in state.queue.indices || toIndex !in state.queue.indices) {
                return@update state
            }
            if (fromIndex == toIndex) {
                return@update state
            }
            val nextQueue = state.queue.toMutableList()
            val song = nextQueue.removeAt(fromIndex)
            nextQueue.add(toIndex, song)

            val nextCurrentIndex = when {
                state.index == fromIndex -> toIndex
                fromIndex < state.index && toIndex >= state.index -> state.index - 1
                fromIndex > state.index && toIndex <= state.index -> state.index + 1
                else -> state.index
            }.coerceIn(nextQueue.indices)

            PlaybackQueueState(queue = nextQueue, index = nextCurrentIndex)
        }
    }

    fun clearQueue() {
        _queueState.value = PlaybackQueueState()
    }
}
