package com.jgm90.cloudmusic.feature.album.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jgm90.cloudmusic.core.innertube.YouTubeRepository
import com.jgm90.cloudmusic.core.model.SongModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumUiState(
    val albumId: String? = null,
    val albumName: String = "",
    val albumArtist: String = "",
    val albumImageUrl: String = "",
    val songs: List<SongModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun loadAlbum(albumName: String, albumId: String?, seedArtistName: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    albumId = albumId,
                    albumName = albumName,
                    albumArtist = seedArtistName.orEmpty(),
                    songs = emptyList(),
                    isLoading = true,
                    error = null,
                )
            }

            val resolvedAlbum = if (albumId.isNullOrBlank() && albumName.isNotBlank()) {
                youTubeRepository.searchAlbums(albumName, limit = 1).firstOrNull()
            } else {
                null
            }

            val resolvedId = albumId ?: resolvedAlbum?.id
            if (resolvedId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Album not found",
                    )
                }
                return@launch
            }

            val songs = youTubeRepository.getAlbumSongs(resolvedId)

            _uiState.update {
                it.copy(
                    albumId = resolvedId,
                    albumName = resolvedAlbum?.name ?: albumName,
                    albumArtist = resolvedAlbum?.artistName ?: seedArtistName.orEmpty(),
                    albumImageUrl = resolvedAlbum?.thumbnailUrl.orEmpty(),
                    songs = songs,
                    isLoading = false,
                    error = null,
                )
            }
        }
    }
}
