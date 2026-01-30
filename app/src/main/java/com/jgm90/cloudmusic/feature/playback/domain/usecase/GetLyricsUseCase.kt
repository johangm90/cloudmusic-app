package com.jgm90.cloudmusic.feature.playback.domain.usecase

import com.jgm90.cloudmusic.core.innertube.YouTubeRepository
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.network.RestInterface
import com.jgm90.cloudmusic.core.playback.LyricLine
import com.jgm90.cloudmusic.core.playback.Lyrics
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetLyricsUseCase @Inject constructor(
    private val restInterface: RestInterface,
    private val youTubeRepository: YouTubeRepository,
) {
    suspend fun execute(song: SongModel): List<LyricLine>? = withContext(Dispatchers.IO) {
        val localLyric = song.local_lyric
        if (!localLyric.isNullOrEmpty()) {
            return@withContext Lyrics.parse(localLyric)?.takeIf { it.isNotEmpty() }
        }

        val lyricText = if (song.isYouTubeSource()) {
            val lyricId = song.lyric_id ?: song.id
            if (lyricId.isNullOrBlank()) {
                null
            } else {
                youTubeRepository.getLyrics(lyricId)?.lyric
            }
        } else {
            restInterface.getLyrics(song.id)?.lyric
        }

        lyricText?.let { Lyrics.parse(it) }?.takeIf { it.isNotEmpty() }
    }
}
