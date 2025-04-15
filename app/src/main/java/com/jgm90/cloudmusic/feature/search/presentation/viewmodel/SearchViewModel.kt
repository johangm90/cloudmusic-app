package com.jgm90.cloudmusic.feature.search.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jgm90.cloudmusic.utils.RestClient
import com.jgm90.cloudmusic.utils.SharedUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchViewState())
    val uiState = _uiState.asStateFlow()

    fun search(query: String) {
        val api = RestClient.build(SharedUtils.server)
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isLoading = true) }
                api.getSongs(query, 1, 50)
            }.onSuccess { result ->
                Log.d("SEARCH", "search: $result")
                _uiState.update {
                    it.copy(
                        searchText = query,
                        isLoading = false,
                        searchResults = result
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
}