package com.jgm90.cloudmusic.feature.artist.presentation.viewmodel

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

data class ArtistUiState(
    val artistId: String? = null,
    val artistName: String = "",
    val artistImageUrl: String = "",
    val songs: List<SongModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    fun loadArtist(artistName: String, artistId: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    artistId = artistId,
                    artistName = artistName,
                    songs = emptyList(),
                    isLoading = true,
                    error = null,
                )
            }

            val resolvedArtist = if (artistId.isNullOrBlank() && artistName.isNotBlank()) {
                youTubeRepository.searchArtists(artistName, limit = 1).firstOrNull()
            } else {
                null
            }

            val resolvedId = artistId ?: resolvedArtist?.id
            if (resolvedId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Artist not found",
                    )
                }
                return@launch
            }

            val songs = youTubeRepository.getArtistTopSongs(resolvedId)

            _uiState.update {
                it.copy(
                    artistId = resolvedId,
                    artistName = resolvedArtist?.name ?: artistName,
                    artistImageUrl = resolvedArtist?.thumbnailUrl.orEmpty(),
                    songs = songs,
                    isLoading = false,
                    error = null,
                )
            }
        }
    }
}
