package com.jgm90.cloudmusic.core.event

data class PlaybackInfoEvent(
    val title: String,
    val artist: String,
    val artUrl: String,
    val isPlaying: Boolean,
) : AppEvent
