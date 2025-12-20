package com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.feature.playlist.data.PlaylistRepository
import com.jgm90.cloudmusic.feature.playlist.data.SongRepository
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
) : ViewModel() {
    fun loadPlaylists(onResult: (List<PlaylistModel>) -> Unit) {
        viewModelScope.launch {
            val playlists = playlistRepository.getAll()
            withContext(Dispatchers.Main) { onResult(playlists) }
        }
    }

    fun loadSongs(playlistId: Int, onResult: (List<SongModel>) -> Unit) {
        viewModelScope.launch {
            val songs = songRepository.getByPlaylist(playlistId)
            withContext(Dispatchers.Main) { onResult(songs) }
        }
    }

    fun savePlaylist(model: PlaylistModel, onDone: () -> Unit) {
        viewModelScope.launch {
            if (model.playlist_id == 0) {
                playlistRepository.insert(model)
            } else {
                playlistRepository.update(model)
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun updatePlaylistOffline(playlistId: Int, offline: Int, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val playlist = playlistRepository.getById(playlistId) ?: return@launch
            playlist.offline = offline
            playlistRepository.update(playlist)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun deletePlaylist(model: PlaylistModel, onDone: () -> Unit) {
        viewModelScope.launch {
            playlistRepository.delete(model)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun updateSong(model: SongModel) {
        viewModelScope.launch {
            songRepository.update(model)
        }
    }

    fun deleteSong(model: SongModel, onDone: () -> Unit) {
        viewModelScope.launch {
            songRepository.delete(model)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun addSongToPlaylist(song: SongModel, playlistId: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            val position = songRepository.getNextPosition()
            val newSong = song.copy(
                local_file = "",
                local_thumbnail = "",
                local_lyric = "",
                position = position,
                position_date = com.jgm90.cloudmusic.core.util.SharedUtils.dateTime,
                playlist_id = playlistId,
            )
            songRepository.insert(newSong)
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
