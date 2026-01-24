package com.jgm90.cloudmusic.core.playback

import com.jgm90.cloudmusic.core.model.SongModel

class PlaybackQueue {
    private val queue = mutableListOf<SongModel>()
    private val originalQueue = mutableListOf<SongModel>()
    private var currentIndex = 0
    private var isShuffled = false

    fun setQueue(songs: List<SongModel>, startIndex: Int = 0) {
        queue.clear()
        originalQueue.clear()
        queue.addAll(songs)
        originalQueue.addAll(songs)
        currentIndex = startIndex.coerceIn(0, songs.size - 1)
        isShuffled = false
    }

    fun addToQueue(song: SongModel) {
        queue.add(song)
        if (!isShuffled) {
            originalQueue.add(song)
        }
    }

    fun addToQueueNext(song: SongModel) {
        val insertIndex = (currentIndex + 1).coerceAtMost(queue.size)
        queue.add(insertIndex, song)
        if (!isShuffled) {
            originalQueue.add(insertIndex, song)
        }
    }

    fun removeFromQueue(index: Int) {
        if (index in queue.indices) {
            val song = queue[index]
            queue.removeAt(index)
            if (!isShuffled) {
                originalQueue.remove(song)
            }
            if (index < currentIndex) {
                currentIndex--
            }
        }
    }

    fun shuffle() {
        if (queue.isEmpty()) return
        val currentSong = getCurrentSong()
        queue.shuffle()
        currentSong?.let { song ->
            val newIndex = queue.indexOf(song)
            if (newIndex != -1) {
                currentIndex = newIndex
            }
        }
        isShuffled = true
    }

    fun unshuffle() {
        if (queue.isEmpty() || !isShuffled) return
        val currentSong = getCurrentSong()
        queue.clear()
        queue.addAll(originalQueue)
        currentSong?.let { song ->
            val newIndex = queue.indexOf(song)
            if (newIndex != -1) {
                currentIndex = newIndex
            }
        }
        isShuffled = false
    }

    fun next(): SongModel? {
        if (queue.isEmpty()) return null
        currentIndex = (currentIndex + 1) % queue.size
        return queue[currentIndex]
    }

    fun previous(): SongModel? {
        if (queue.isEmpty()) return null
        currentIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
        return queue[currentIndex]
    }

    fun getCurrentSong(): SongModel? {
        return if (currentIndex in queue.indices) queue[currentIndex] else null
    }

    fun getCurrentIndex(): Int = currentIndex

    fun getQueue(): List<SongModel> = queue.toList()

    fun size(): Int = queue.size

    fun isEmpty(): Boolean = queue.isEmpty()

    fun peekNext(): SongModel? {
        if (queue.isEmpty()) return null
        val nextIndex = (currentIndex + 1) % queue.size
        return if (nextIndex in queue.indices) queue[nextIndex] else null
    }

    fun peekPrevious(): SongModel? {
        if (queue.isEmpty()) return null
        val prevIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
        return if (prevIndex in queue.indices) queue[prevIndex] else null
    }

    fun skipTo(index: Int): SongModel? {
        if (index !in queue.indices) return null
        currentIndex = index
        return queue[currentIndex]
    }

    fun clear() {
        queue.clear()
        originalQueue.clear()
        currentIndex = 0
        isShuffled = false
    }
}
