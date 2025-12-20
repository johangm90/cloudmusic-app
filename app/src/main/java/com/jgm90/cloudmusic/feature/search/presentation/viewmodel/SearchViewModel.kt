package com.jgm90.cloudmusic.feature.search.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jgm90.cloudmusic.core.network.RestInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val api: RestInterface,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchViewState())
    val uiState = _uiState.asStateFlow()

    fun search(query: String) {
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
