package com.jgm90.cloudmusic.core.model

import com.jgm90.cloudmusic.core.innertube.mapper.YouTubeMapper

data class SongModel(
    val id: String?,
    val name: String,
    val artist: List<String> = emptyList(),
    val album: String,
    val pic_id: String?,  // Can be a full URL (YouTube Music) or video ID (fallback)
    val url_id: String?,
    val lyric_id: String?,
    val source: String?,
    var local_file: String? = null,
    var local_thumbnail: String? = null,
    var local_lyric: String? = null,
    var position: Int = 0,
    var position_date: String? = null,
    val playlist_id: Int = 0,
) {
    /**
     * Get cover/album art thumbnail URL
     * For YouTube Music sources, pic_id contains the full album art URL
     * For video fallbacks, it's a video ID used to construct thumbnail URL
     */
    fun getCoverThumbnail(): String {
        // Check for local thumbnail first
        if (!local_thumbnail.isNullOrEmpty()) {
            return local_thumbnail!!
        }

        if (pic_id.isNullOrEmpty()) {
            return ""
        }

        // If pic_id is already a full URL (YouTube Music album art)
        if (pic_id.startsWith("http")) {
            return pic_id
        }

        // Otherwise, treat as video ID and construct YouTube thumbnail URL
        return YouTubeMapper.getVideoThumbnailUrl(pic_id, YouTubeMapper.ThumbnailQuality.HIGH)
    }

    /**
     * Get high quality cover/album art URL
     */
    fun getHighQualityThumbnail(): String {
        if (!local_thumbnail.isNullOrEmpty()) {
            return local_thumbnail!!
        }

        if (pic_id.isNullOrEmpty()) {
            return ""
        }

        // If pic_id is already a full URL, try to scale it up
        if (pic_id.startsWith("http")) {
            return YouTubeMapper.getHighQualityAlbumArt(pic_id)
        }

        // Otherwise, treat as video ID
        return YouTubeMapper.getVideoThumbnailUrl(pic_id, YouTubeMapper.ThumbnailQuality.MAX)
    }

    /**
     * Get audio URL - for YouTube sources this should be fetched via YouTubeRepository
     */
    fun getAudioUrl(): String {
        // Local file takes priority
        if (!local_file.isNullOrEmpty()) {
            return local_file!!
        }
        // For YouTube, the actual stream URL needs to be fetched dynamically
        // Use YouTubeRepository.getStreamUrl() for actual streaming
        return ""
    }

    fun getFileName(): String {
        var filename = "$name.mp3"
        filename = filename.replace(
            "\\\\|>|<|\"|\\||\\*|\\?|%|:|#|/".toRegex(),
            ""
        )
        return filename
    }

    fun isYouTubeSource(): Boolean {
        return source == "youtube" || source == "youtube_music"
    }

    fun getArtistString(): String {
        return artist.joinToString(", ")
    }

    /**
     * Check if this song has album art (as opposed to video thumbnail)
     */
    fun hasAlbumArt(): Boolean {
        return source == "youtube_music" && !pic_id.isNullOrEmpty() && pic_id.startsWith("http")
    }
}
