package com.jgm90.cloudmusic.core.playback

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class PlaybackCommand {
    object TogglePlayPause : PlaybackCommand()
}

@Singleton
class PlaybackCommandController @Inject constructor() {
    private val _commands = MutableSharedFlow<PlaybackCommand>(extraBufferCapacity = 1)
    val commands = _commands.asSharedFlow()

    fun togglePlayPause() {
        _commands.tryEmit(PlaybackCommand.TogglePlayPause)
    }
}
