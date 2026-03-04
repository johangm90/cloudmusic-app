package com.jgm90.cloudmusic.feature.discover.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jgm90.cloudmusic.core.innertube.YouTubeRepository
import com.jgm90.cloudmusic.core.model.SongModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val homeSongs: List<SongModel> = emptyList(),
    val chartSongs: List<SongModel> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val homeDeferred = async { youTubeRepository.browseHome().take(30) }
            val chartsDeferred = async { youTubeRepository.browseCharts().take(30) }
            val home = runCatching { homeDeferred.await() }.getOrDefault(emptyList())
            val charts = runCatching { chartsDeferred.await() }.getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    homeSongs = home,
                    chartSongs = charts,
                    error = if (home.isEmpty() && charts.isEmpty()) "No discovery content available" else null,
                )
            }
        }
    }
}
