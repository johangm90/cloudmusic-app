package com.jgm90.cloudmusic.core.event

data class PlaybackErrorEvent(
    val message: String,
    val retryCount: Int,
    val maxRetries: Int
) : AppEvent
