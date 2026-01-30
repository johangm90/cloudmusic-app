package com.jgm90.cloudmusic.core.download

import com.jgm90.cloudmusic.core.event.DownloadEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class DownloadEventController @Inject constructor() {
    private val _events = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    fun emit(event: DownloadEvent) {
        _events.tryEmit(event)
    }
}
