package com.jgm90.cloudmusic.core.innertube.mapper

import com.jgm90.cloudmusic.core.innertube.models.*
import com.jgm90.cloudmusic.core.model.SongModel

/**
 * Result of a search operation with pagination info
 */
data class SearchResult(
    val songs: List<SongModel>,
    val continuation: String? = null,
    val hasMore: Boolean = false
)

object YouTubeMapper {

    /**
     * Parse YouTube Music search results (WEB_REMIX client)
     * Extracts songs with proper album art from musicThumbnailRenderer
     */
    fun mapMusicSearchResponseToSongs(response: SearchResponse): SearchResult {
        val songs = mutableListOf<SongModel>()
        var continuation: String? = null

        // YouTube Music uses tabbedSearchResultsRenderer
        val tabs = response.contents?.tabbedSearchResultsRenderer?.tabs
        val firstTab = tabs?.firstOrNull()?.tabRenderer
        val sectionListRenderer = firstTab?.content?.sectionListRenderer

        sectionListRenderer?.contents?.forEach { section ->
            val musicShelf = section.musicShelfRenderer
            musicShelf?.contents?.forEach { content ->
                content.musicResponsiveListItemRenderer?.let { renderer ->
                    mapMusicResponsiveListItemToSong(renderer)?.let { songs.add(it) }
                }
            }

            // Get continuation token for pagination
            musicShelf?.continuations?.firstOrNull()?.let { cont ->
                continuation = cont.nextContinuationData?.continuation
                    ?: cont.reloadContinuationData?.continuation
            }
        }

        // Also check for continuation at section list level
        if (continuation == null) {
            sectionListRenderer?.continuations?.firstOrNull()?.let { cont ->
                continuation = cont.nextContinuationData?.continuation
                    ?: cont.reloadContinuationData?.continuation
            }
        }

        return SearchResult(
            songs = songs,
            continuation = continuation,
            hasMore = continuation != null
        )
    }

    /**
     * Parse continuation response for pagination
     */
    fun mapContinuationResponseToSongs(response: SearchResponse): SearchResult {
        val songs = mutableListOf<SongModel>()
        var continuation: String? = null

        response.continuationContents?.musicShelfContinuation?.let { musicShelf ->
            musicShelf.contents?.forEach { content ->
                content.musicResponsiveListItemRenderer?.let { renderer ->
                    mapMusicResponsiveListItemToSong(renderer)?.let { songs.add(it) }
                }
            }
            musicShelf.continuations?.firstOrNull()?.let { cont ->
                continuation = cont.nextContinuationData?.continuation
                    ?: cont.reloadContinuationData?.continuation
            }
        }

        return SearchResult(
            songs = songs,
            continuation = continuation,
            hasMore = continuation != null
        )
    }

    /**
     * Map a single music responsive list item to SongModel
     * Extracts album art from musicThumbnailRenderer
     */
    private fun mapMusicResponsiveListItemToSong(renderer: MusicResponsiveListItemRenderer): SongModel? {
        // Extract video ID from various possible locations
        val videoId = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
            ?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
            ?: renderer.playlistItemData?.videoId
            ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
            ?: return null

        val flexColumns = renderer.flexColumns ?: return null

        // Column 0: Title
        val title = flexColumns.getOrNull(0)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.getText()
            ?: return null

        // Column 1: Artist • Album (parse from runs)
        var artist = ""
        var album = ""

        flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.forEach { run ->
            val pageType = run.navigationEndpoint?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig?.pageType

            val text = run.text ?: ""

            when (pageType) {
                "MUSIC_PAGE_TYPE_ARTIST" -> artist = text
                "MUSIC_PAGE_TYPE_ALBUM" -> album = text
            }
        }

        // Fallback: use first run as artist if not detected
        if (artist.isEmpty()) {
            artist = flexColumns.getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                ?.firstOrNull()?.text ?: ""
        }

        // Extract album art thumbnail URL
        val thumbnailUrl = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
            ?.lastOrNull()?.url
            ?.let { scaleAlbumArtUrl(it, 500) }
            ?: ""

        return SongModel(
            id = videoId,
            name = title,
            artist = if (artist.isNotEmpty()) listOf(artist) else emptyList(),
            album = album,
            pic_id = thumbnailUrl,  // Store full thumbnail URL directly
            url_id = videoId,
            lyric_id = videoId,
            source = "youtube_music"
        )
    }

    /**
     * Scale album art URL to desired size
     * YouTube Music thumbnails use w60-h60, w120-h120 format
     */
    private fun scaleAlbumArtUrl(url: String, size: Int): String {
        return url
            .replace(Regex("w\\d+-h\\d+"), "w$size-h$size")
            .replace(Regex("=w\\d+-h\\d+"), "=w$size-h$size")
    }

    /**
     * Get high quality album art URL
     */
    fun getHighQualityAlbumArt(url: String): String {
        return scaleAlbumArtUrl(url, 500)
    }

    /**
     * Get medium quality album art URL
     */
    fun getMediumQualityAlbumArt(url: String): String {
        return scaleAlbumArtUrl(url, 226)
    }

    /**
     * Map player response to stream URL
     * Prefers audio-only formats with highest bitrate
     */
    fun mapPlayerResponseToStreamUrl(response: PlayerResponse): String? {
        if (response.playabilityStatus?.isPlayable() != true) {
            return null
        }

        val streamingData = response.streamingData ?: return null

        // Find best audio-only format (highest bitrate)
        val audioFormats = streamingData.adaptiveFormats
            ?.filter { it.isAudioOnly() && it.url != null }
            ?.sortedByDescending { it.bitrate ?: 0 }

        audioFormats?.firstOrNull()?.url?.let { return it }

        // Fallback to combined formats
        streamingData.formats
            ?.filter { it.url != null }
            ?.sortedByDescending { it.bitrate ?: 0 }
            ?.firstOrNull()?.url?.let { return it }

        // Try HLS manifest
        streamingData.hlsManifestUrl?.let { return it }

        return null
    }

    /**
     * Map player response to song details
     */
    fun mapPlayerResponseToSongDetails(response: PlayerResponse, existingSong: SongModel? = null): SongModel? {
        val videoDetails = response.videoDetails ?: return null
        val videoId = videoDetails.videoId ?: return null

        // Clean up title and author
        val title = videoDetails.title
            ?.replace("(Official Music Video)", "")
            ?.replace("(Official Video)", "")
            ?.replace("(Audio)", "")
            ?.replace("(Lyrics)", "")
            ?.trim()
            ?: existingSong?.name ?: "Unknown"

        val author = videoDetails.author
            ?.replace(" - Topic", "")
            ?.trim()
            ?: existingSong?.artist?.firstOrNull() ?: "Unknown"

        return SongModel(
            id = videoId,
            name = title,
            artist = listOf(author),
            album = existingSong?.album ?: "",
            pic_id = existingSong?.pic_id ?: getVideoThumbnailUrl(videoId),
            url_id = videoId,
            lyric_id = videoId,
            source = if (videoDetails.musicVideoType != null) "youtube_music" else "youtube"
        )
    }

    /**
     * Get video thumbnail URL (fallback when no album art)
     */
    fun getVideoThumbnailUrl(videoId: String, quality: ThumbnailQuality = ThumbnailQuality.HIGH): String {
        return when (quality) {
            ThumbnailQuality.DEFAULT -> "https://i.ytimg.com/vi/$videoId/default.jpg"
            ThumbnailQuality.MEDIUM -> "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
            ThumbnailQuality.HIGH -> "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            ThumbnailQuality.MAX -> "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        }
    }

    enum class ThumbnailQuality {
        DEFAULT,  // 120x90
        MEDIUM,   // 320x180
        HIGH,     // 480x360
        MAX       // 1280x720 or higher
    }

    /**
     * Map next response to related songs
     */
    fun mapNextResponseToRelatedSongs(response: NextResponse): List<SongModel> {
        val songs = mutableListOf<SongModel>()

        response.contents?.twoColumnWatchNextResults?.secondaryResults?.secondaryResults?.results?.forEach { item ->
            item.compactVideoRenderer?.let { video ->
                mapCompactVideoRendererToSong(video)?.let { songs.add(it) }
            }
        }

        return songs
    }

    private fun mapCompactVideoRendererToSong(video: CompactVideoRenderer): SongModel? {
        val videoId = video.videoId ?: return null
        val title = video.title?.getText() ?: return null
        val artist = video.shortBylineText?.getText() ?: "Unknown"

        return SongModel(
            id = videoId,
            name = title,
            artist = listOf(artist),
            album = "",
            pic_id = getVideoThumbnailUrl(videoId),
            url_id = videoId,
            lyric_id = videoId,
            source = "youtube"
        )
    }

    /**
     * Map browse response to songs (YouTube Music home, charts, etc.)
     */
    fun mapBrowseResponseToSongs(response: BrowseResponse): List<SongModel> {
        val songs = mutableListOf<SongModel>()

        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.forEach { tab ->
            tab.tabRenderer?.content?.sectionListRenderer?.contents?.forEach { section ->
                section.musicShelfRenderer?.contents?.forEach { content ->
                    content.musicResponsiveListItemRenderer?.let { renderer ->
                        mapMusicResponsiveListItemToSong(renderer)?.let { songs.add(it) }
                    }
                    content.musicTwoRowItemRenderer?.let { renderer ->
                        mapMusicTwoRowItemToSong(renderer)?.let { songs.add(it) }
                    }
                }
            }
        }

        response.contents?.sectionListRenderer?.contents?.forEach { section ->
            section.musicShelfRenderer?.contents?.forEach { content ->
                content.musicResponsiveListItemRenderer?.let { renderer ->
                    mapMusicResponsiveListItemToSong(renderer)?.let { songs.add(it) }
                }
            }
        }

        return songs
    }

    private fun mapMusicTwoRowItemToSong(renderer: MusicTwoRowItemRenderer): SongModel? {
        val videoId = renderer.navigationEndpoint?.watchEndpoint?.videoId ?: return null
        val title = renderer.title?.getText() ?: return null
        val subtitle = renderer.subtitle?.getText() ?: ""

        val parts = subtitle.split(" • ", " · ")
        val artist = parts.getOrNull(0) ?: "Unknown"

        val thumbnailUrl = renderer.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails
            ?.lastOrNull()?.url
            ?.let { scaleAlbumArtUrl(it, 500) }
            ?: getVideoThumbnailUrl(videoId)

        return SongModel(
            id = videoId,
            name = title,
            artist = listOf(artist),
            album = "",
            pic_id = thumbnailUrl,
            url_id = videoId,
            lyric_id = videoId,
            source = "youtube_music"
        )
    }

    /**
     * Map queue response to songs
     */
    fun mapQueueResponseToSongs(response: MusicQueueResponse): List<SongModel> {
        val songs = mutableListOf<SongModel>()

        response.queueDatas?.forEach { queueData ->
            queueData.content?.playlistPanelVideoRenderer?.let { renderer ->
                val videoId = renderer.videoId ?: return@forEach
                val title = renderer.title?.getText() ?: return@forEach
                val artist = renderer.longBylineText?.getText() ?: ""

                val thumbnailUrl = renderer.thumbnail?.thumbnails?.lastOrNull()?.url
                    ?.let { scaleAlbumArtUrl(it, 500) }
                    ?: getVideoThumbnailUrl(videoId)

                songs.add(SongModel(
                    id = videoId,
                    name = title,
                    artist = if (artist.isNotEmpty()) listOf(artist) else emptyList(),
                    album = "",
                    pic_id = thumbnailUrl,
                    url_id = videoId,
                    lyric_id = videoId,
                    source = "youtube_music"
                ))
            }
        }

        return songs
    }

    // Legacy compatibility methods

    @Deprecated("Use mapMusicSearchResponseToSongs for YouTube Music search")
    fun mapSearchResponseToSongs(response: SearchResponse): List<SongModel> {
        return mapMusicSearchResponseToSongs(response).songs
    }

    @Deprecated("Use pic_id directly as it now contains the full URL")
    fun getBestThumbnailUrl(videoId: String, preferHighQuality: Boolean = true): String {
        return if (preferHighQuality) {
            getVideoThumbnailUrl(videoId, ThumbnailQuality.MAX)
        } else {
            getVideoThumbnailUrl(videoId, ThumbnailQuality.HIGH)
        }
    }
}
