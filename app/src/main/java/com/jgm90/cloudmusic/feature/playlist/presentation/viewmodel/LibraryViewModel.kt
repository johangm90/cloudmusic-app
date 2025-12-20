package com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jgm90.cloudmusic.core.data.local.repository.LibraryRepository
import com.jgm90.cloudmusic.core.model.SongModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {
    fun loadRecent(onResult: (List<SongModel>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getRecent())
        }
    }

    fun loadLiked(onResult: (List<SongModel>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getLiked())
        }
    }
}
