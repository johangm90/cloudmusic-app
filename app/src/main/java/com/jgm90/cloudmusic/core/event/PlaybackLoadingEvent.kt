package com.jgm90.cloudmusic.core.event

data class PlaybackLoadingEvent(
    val isLoading: Boolean,
) : AppEvent
