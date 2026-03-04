package com.jgm90.cloudmusic.core.innertube.mapper

import com.jgm90.cloudmusic.core.innertube.models.*
import com.jgm90.cloudmusic.core.model.AlbumModel
import com.jgm90.cloudmusic.core.model.ArtistModel
import com.jgm90.cloudmusic.core.model.SongModel
import java.net.URLDecoder

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
     * Parse YouTube Music artist search results
     */
    fun mapMusicSearchResponseToArtists(response: SearchResponse): List<ArtistModel> {
        val artists = mutableListOf<ArtistModel>()

        val tabs = response.contents?.tabbedSearchResultsRenderer?.tabs
        val firstTab = tabs?.firstOrNull()?.tabRenderer
        val sectionListRenderer = firstTab?.content?.sectionListRenderer

        sectionListRenderer?.contents?.forEach { section ->
            section.musicShelfRenderer?.contents?.forEach { content ->
                content.musicResponsiveListItemRenderer?.let { renderer ->
                    mapMusicResponsiveListItemToArtist(renderer)?.let { artists.add(it) }
                }
            }
        }

        return artists.distinctBy { it.id }
    }

    /**
     * Parse YouTube Music album search results
     */
    fun mapMusicSearchResponseToAlbums(response: SearchResponse): List<AlbumModel> {
        val albums = mutableListOf<AlbumModel>()

        val tabs = response.contents?.tabbedSearchResultsRenderer?.tabs
        val firstTab = tabs?.firstOrNull()?.tabRenderer
        val sectionListRenderer = firstTab?.content?.sectionListRenderer

        sectionListRenderer?.contents?.forEach { section ->
            section.musicShelfRenderer?.contents?.forEach { content ->
                content.musicResponsiveListItemRenderer?.let { renderer ->
                    mapMusicResponsiveListItemToAlbum(renderer)?.let { albums.add(it) }
                }
            }
        }

        return albums.distinctBy { it.id }
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
        var artistId: String? = null
        var album = ""
        var albumId: String? = null

        flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.forEach { run ->
            val pageType = run.navigationEndpoint?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig?.pageType

            val text = run.text ?: ""

            when (pageType) {
                "MUSIC_PAGE_TYPE_ARTIST" -> {
                    artist = text
                    artistId = run.navigationEndpoint?.browseEndpoint?.browseId ?: artistId
                }
                "MUSIC_PAGE_TYPE_ALBUM" -> {
                    album = text
                    albumId = run.navigationEndpoint?.browseEndpoint?.browseId ?: albumId
                }
            }
        }

        // Fallback: use first run as artist if not detected
        if (artist.isEmpty()) {
            val firstRun = flexColumns.getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                ?.firstOrNull()
            artist = firstRun?.text ?: ""
            artistId = firstRun?.navigationEndpoint?.browseEndpoint?.browseId ?: artistId
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
            artist_id = artistId,
            album = album,
            album_id = albumId,
            pic_id = thumbnailUrl,  // Store full thumbnail URL directly
            url_id = videoId,
            lyric_id = videoId,
            source = "youtube_music"
        )
    }


    private fun mapMusicResponsiveListItemToArtist(renderer: MusicResponsiveListItemRenderer): ArtistModel? {
        val flexColumns = renderer.flexColumns ?: return null
        val title = flexColumns.getOrNull(0)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.getText()
            ?.trim()
            .orEmpty()
        if (title.isEmpty()) {
            return null
        }

        val subtitle = flexColumns.getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.getText()
            ?.trim()
            .orEmpty()

        var artistId = renderer.navigationEndpoint?.browseEndpoint?.browseId
        flexColumns.getOrNull(0)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
            ?.forEach { run ->
                val pageType = run.navigationEndpoint?.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig?.pageType
                if (pageType == "MUSIC_PAGE_TYPE_ARTIST") {
                    artistId = run.navigationEndpoint?.browseEndpoint?.browseId ?: artistId
                }
            }
        flexColumns.getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
            ?.forEach { run ->
                val pageType = run.navigationEndpoint?.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig?.pageType
                if (pageType == "MUSIC_PAGE_TYPE_ARTIST") {
                    artistId = run.navigationEndpoint?.browseEndpoint?.browseId ?: artistId
                }
            }

        if (artistId.isNullOrBlank()) {
            return null
        }

        val thumbnailUrl = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
            ?.lastOrNull()?.url
            ?.let { scaleAlbumArtUrl(it, 500) }
            ?: ""

        return ArtistModel(
            id = artistId,
            name = title,
            thumbnailUrl = thumbnailUrl,
            subtitle = subtitle.ifBlank { null },
        )
    }

    private fun mapMusicResponsiveListItemToAlbum(renderer: MusicResponsiveListItemRenderer): AlbumModel? {
        val flexColumns = renderer.flexColumns ?: return null
        val title = flexColumns.getOrNull(0)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.getText()
            ?.trim()
            .orEmpty()
        if (title.isEmpty()) {
            return null
        }

        var albumId = renderer.navigationEndpoint?.browseEndpoint?.browseId
        var artistName = ""

        flexColumns.getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
            ?.forEach { run ->
                val pageType = run.navigationEndpoint?.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig?.pageType
                when (pageType) {
                    "MUSIC_PAGE_TYPE_ALBUM" -> {
                        albumId = run.navigationEndpoint?.browseEndpoint?.browseId ?: albumId
                    }
                    "MUSIC_PAGE_TYPE_ARTIST" -> {
                        if (artistName.isBlank()) {
                            artistName = run.text.orEmpty()
                        }
                    }
                }
            }

        if (albumId.isNullOrBlank()) {
            return null
        }

        val thumbnailUrl = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
            ?.lastOrNull()?.url
            ?.let { scaleAlbumArtUrl(it, 500) }
            ?: ""

        return AlbumModel(
            id = albumId,
            name = title,
            artistName = artistName,
            thumbnailUrl = thumbnailUrl,
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
        val formats = streamingData.formats.orEmpty()
        val adaptiveFormats = streamingData.adaptiveFormats.orEmpty()

        // Prefer progressive mp4 (muxed audio+video) to maximize MediaPlayer compatibility.
        formats
            .filter { it.mimeType?.contains("video/mp4") == true && it.audioQuality != null }
            .maxByOrNull { it.bitrate ?: 0 }
            ?.resolvedUrl()
            ?.let { return it }

        // Fallback to any audio-only format if mp4 not available.
        val audioFormats = adaptiveFormats.filter { it.isAudioOnly() }

        audioFormats
            .filter { it.mimeType?.contains("audio/mp4") == true }
            .maxByOrNull { it.bitrate ?: 0 }
            ?.resolvedUrl()
            ?.let { return it }

        // Fallback to any audio-only format.
        audioFormats.maxByOrNull { it.bitrate ?: 0 }
            ?.resolvedUrl()
            ?.let { return it }

        // Fallback to combined formats.
        formats
            .maxByOrNull { it.bitrate ?: 0 }
            ?.resolvedUrl()
            ?.let { return it }

        // Try HLS manifest.
        streamingData.hlsManifestUrl?.let { return it }

        return null
    }

    private fun Format.resolvedUrl(): String? {
        if (!url.isNullOrBlank()) {
            return url
        }
        return extractUrlFromCipher(signatureCipher ?: cipher)
    }

    private fun extractUrlFromCipher(cipher: String?): String? {
        if (cipher.isNullOrBlank()) {
            return null
        }
        val params = cipher.split("&")
            .mapNotNull { part ->
                val index = part.indexOf('=')
                if (index <= 0) {
                    null
                } else {
                    val key = part.substring(0, index)
                    val value = part.substring(index + 1)
                    key to URLDecoder.decode(value, Charsets.UTF_8.name())
                }
            }
            .toMap()

        val decodedUrl = params["url"] ?: return null
        val signature = params["sig"] ?: params["signature"]
        val sp = params["sp"].orEmpty().ifBlank { "signature" }

        if (signature.isNullOrBlank()) {
            if (!params["s"].isNullOrBlank()) {
                return null
            }
            return decodedUrl
        }
        if (decodedUrl.contains("$sp=")) {
            return decodedUrl
        }
        val separator = if (decodedUrl.contains("?")) "&" else "?"
        return "$decodedUrl$separator$sp=$signature"
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
            album_id = existingSong?.album_id,
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
            album_id = null,
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
            album_id = null,
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
                    artist_id = null,
                    album = "",
                    album_id = null,
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
