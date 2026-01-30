package com.jgm90.cloudmusic.feature.playback.domain.usecase

import com.jgm90.cloudmusic.core.data.local.repository.LibraryRepository
import com.jgm90.cloudmusic.core.model.SongModel
import javax.inject.Inject

class PlaybackLibraryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    suspend fun addRecent(song: SongModel) {
        libraryRepository.addRecent(song)
    }

    suspend fun isLiked(songId: String?): Boolean {
        return libraryRepository.isLiked(songId)
    }

    suspend fun toggleLiked(song: SongModel): Boolean {
        return libraryRepository.toggleLiked(song)
    }
}
