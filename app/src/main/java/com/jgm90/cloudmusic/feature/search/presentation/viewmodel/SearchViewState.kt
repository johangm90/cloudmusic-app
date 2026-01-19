package com.jgm90.cloudmusic.feature.search.presentation.viewmodel

import com.jgm90.cloudmusic.core.model.SongModel

data class SearchViewState(
    val searchText: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchResults: List<SongModel> = emptyList(),
    val continuation: String? = null,
    val hasMore: Boolean = false,
)
