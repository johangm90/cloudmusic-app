package com.jgm90.cloudmusic.feature.search.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jgm90.cloudmusic.core.innertube.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchViewState())
    val uiState = _uiState.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            runCatching {
                _uiState.update {
                    it.copy(
                        searchText = query,
                        isLoading = true,
                        isLoadingMore = false,
                        error = null,
                        searchResults = emptyList(),
                        continuation = null,
                        hasMore = false,
                    )
                }
                youTubeRepository.searchSongsWithPagination(query)
            }.onSuccess { result ->
                Log.d("SEARCH", "search: $result")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        searchResults = result.songs,
                        continuation = result.continuation,
                        hasMore = result.hasMore
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        val query = current.searchText.trim()
        val continuation = current.continuation
        if (query.isEmpty() || continuation.isNullOrEmpty()) return

        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoadingMore = true, error = null) }
                youTubeRepository.searchSongsWithPagination(query, continuation)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        searchResults = it.searchResults + result.songs,
                        continuation = result.continuation,
                        hasMore = result.hasMore
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        error = error.message
                    )
                }
            }
        }
    }
}
