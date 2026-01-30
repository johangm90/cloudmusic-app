package com.jgm90.cloudmusic.core.download

import androidx.lifecycle.ViewModel
import com.jgm90.cloudmusic.core.event.DownloadEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DownloadEventsViewModel @Inject constructor(
    private val downloadEventController: DownloadEventController,
) : ViewModel() {
    fun emit(event: DownloadEvent) {
        downloadEventController.emit(event)
    }
}
