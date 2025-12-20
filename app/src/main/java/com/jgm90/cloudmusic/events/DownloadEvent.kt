package com.jgm90.cloudmusic.events

data class DownloadEvent(
    val destination: Boolean,
    val visibility: Int,
    val url: String,
    val name: String,
    val filename: String,
)
