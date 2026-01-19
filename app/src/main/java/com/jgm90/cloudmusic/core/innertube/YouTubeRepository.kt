package com.jgm90.cloudmusic.core.innertube

import android.util.Log
import com.google.gson.Gson
import com.jgm90.cloudmusic.core.innertube.mapper.SearchResult
import com.jgm90.cloudmusic.core.innertube.mapper.YouTubeMapper
import com.jgm90.cloudmusic.core.model.LyricModel
import com.jgm90.cloudmusic.core.model.SongModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepository @Inject constructor(
    private val innerTube: InnerTube
) {
    companion object {
        private const val TAG = "YouTubeRepository"
        private const val LRCLIB_BASE_URL = "https://lrclib.net/api"
    }

    // HTTP client for LRCLIB
    private val httpClient = OkHttpClient.Builder().build()
    private val gson = Gson()

    // Stream URL cache to avoid repeated player requests
    private val streamUrlCache = mutableMapOf<String, CachedStreamUrl>()

    // Store continuation token for pagination
    private var lastSearchContinuation: String? = null
    private var lastSearchQuery: String? = null

    private data class CachedStreamUrl(
        val url: String,
        val expireTime: Long
    )

    /**
     * Search songs in YouTube Music
     * @param query Search query
     * @param limit Maximum number of results
     * @param offset Number of results to skip (for pagination)
     */
    suspend fun searchSongs(
        query: String,
        limit: Int = 30,
        offset: Int = 0
    ): List<SongModel> {
        Log.d(TAG, "Searching YouTube Music for: $query (limit=$limit, offset=$offset)")

        // If this is a new query, reset continuation
        if (query != lastSearchQuery) {
            lastSearchContinuation = null
            lastSearchQuery = query
        }

        // For offset > 0, we need to use continuation
        return if (offset > 0 && lastSearchContinuation != null) {
            searchWithContinuation(query, limit)
        } else {
            searchInitial(query, limit)
        }
    }

    private suspend fun searchInitial(query: String, limit: Int): List<SongModel> {
        val result = innerTube.searchMusic(
            query = query,
            params = InnerTubeConfig.SearchParams.SONGS_FILTER
        )

        return result.fold(
            onSuccess = { response ->
                val searchResult = YouTubeMapper.mapMusicSearchResponseToSongs(response)
                lastSearchContinuation = searchResult.continuation
                Log.d(TAG, "Found ${searchResult.songs.size} results, hasMore=${searchResult.hasMore}")
                searchResult.songs.take(limit)
            },
            onFailure = { error ->
                Log.e(TAG, "Search failed", error)
                emptyList()
            }
        )
    }

    private suspend fun searchWithContinuation(query: String, limit: Int): List<SongModel> {
        val continuation = lastSearchContinuation ?: return emptyList()

        val result = innerTube.searchMusic(
            query = query,
            params = InnerTubeConfig.SearchParams.SONGS_FILTER,
            continuation = continuation
        )

        return result.fold(
            onSuccess = { response ->
                val searchResult = YouTubeMapper.mapContinuationResponseToSongs(response)
                lastSearchContinuation = searchResult.continuation
                Log.d(TAG, "Continuation: found ${searchResult.songs.size} more results")
                searchResult.songs.take(limit)
            },
            onFailure = { error ->
                Log.e(TAG, "Continuation search failed", error)
                emptyList()
            }
        )
    }

    /**
     * Search with full pagination info
     */
    suspend fun searchSongsWithPagination(
        query: String,
        continuation: String? = null
    ): SearchResult {
        Log.d(TAG, "Searching YouTube Music for: $query (continuation=${continuation != null})")

        val result = innerTube.searchMusic(
            query = query,
            params = InnerTubeConfig.SearchParams.SONGS_FILTER,
            continuation = continuation
        )

        return result.fold(
            onSuccess = { response ->
                if (continuation != null) {
                    YouTubeMapper.mapContinuationResponseToSongs(response)
                } else {
                    YouTubeMapper.mapMusicSearchResponseToSongs(response)
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Search failed", error)
                SearchResult(emptyList(), null, false)
            }
        )
    }

    /**
     * Get stream URL for a video
     */
    suspend fun getStreamUrl(videoId: String): String? {
        // Check cache first
        streamUrlCache[videoId]?.let { cached ->
            if (System.currentTimeMillis() < cached.expireTime) {
                Log.d(TAG, "Using cached stream URL for $videoId")
                return cached.url
            }
        }

        Log.d(TAG, "Getting stream URL for: $videoId")

        val result = innerTube.player(videoId)
        return result.fold(
            onSuccess = { response ->
                val url = YouTubeMapper.mapPlayerResponseToStreamUrl(response)
                if (url != null) {
                    // Cache for 5 hours (YouTube URLs typically expire in 6 hours)
                    val expireTime = System.currentTimeMillis() + (5 * 60 * 60 * 1000)
                    streamUrlCache[videoId] = CachedStreamUrl(url, expireTime)
                    Log.d(TAG, "Got stream URL: ${url.take(100)}...")
                } else {
                    Log.w(TAG, "No stream URL found for $videoId")
                }
                url
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get stream URL", error)
                null
            }
        )
    }

    /**
     * Get song details from player response
     */
    suspend fun getSongDetails(videoId: String): SongModel? {
        Log.d(TAG, "Getting song details for: $videoId")

        val result = innerTube.player(videoId)
        return result.fold(
            onSuccess = { response ->
                YouTubeMapper.mapPlayerResponseToSongDetails(response)
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get song details", error)
                null
            }
        )
    }

    /**
     * Get related songs (music queue)
     */
    suspend fun getRelatedSongs(videoId: String): List<SongModel> {
        Log.d(TAG, "Getting related songs for: $videoId")

        val result = innerTube.musicGetQueue(listOf(videoId))
        return result.fold(
            onSuccess = { response ->
                val songs = YouTubeMapper.mapQueueResponseToSongs(response)
                Log.d(TAG, "Found ${songs.size} related songs")
                songs
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get related songs", error)
                emptyList()
            }
        )
    }

    /**
     * Get search suggestions
     */
    suspend fun getSearchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()

        val result = innerTube.musicSearchSuggestions(query)
        return result.fold(
            onSuccess = { response ->
                response.contents?.flatMap { content ->
                    content.searchSuggestionsSectionRenderer?.contents?.mapNotNull { item ->
                        item.searchSuggestionRenderer?.suggestion?.getText()
                    } ?: emptyList()
                } ?: emptyList()
            },
            onFailure = {
                emptyList()
            }
        )
    }

    /**
     * Browse YouTube Music home
     */
    suspend fun browseHome(): List<SongModel> {
        Log.d(TAG, "Browsing music home")

        val result = innerTube.browse(InnerTubeConfig.BrowseIds.MUSIC_HOME)
        return result.fold(
            onSuccess = { response ->
                YouTubeMapper.mapBrowseResponseToSongs(response)
            },
            onFailure = { error ->
                Log.e(TAG, "Browse home failed", error)
                emptyList()
            }
        )
    }

    /**
     * Browse YouTube Music charts
     */
    suspend fun browseCharts(): List<SongModel> {
        Log.d(TAG, "Browsing music charts")

        val result = innerTube.browse(InnerTubeConfig.BrowseIds.MUSIC_CHARTS)
        return result.fold(
            onSuccess = { response ->
                YouTubeMapper.mapBrowseResponseToSongs(response)
            },
            onFailure = { error ->
                Log.e(TAG, "Browse charts failed", error)
                emptyList()
            }
        )
    }

    /**
     * Get lyrics from LRCLIB
     */
    suspend fun getLyrics(videoId: String): LyricModel? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting lyrics for: $videoId")

        // First, get song details to extract title and artist
        val songDetails = getSongDetails(videoId) ?: return@withContext null

        val title = songDetails.name
        val artist = songDetails.artist.firstOrNull() ?: ""

        if (title.isBlank()) return@withContext null

        try {
            val url = "$LRCLIB_BASE_URL/search?track_name=${
                java.net.URLEncoder.encode(title, "UTF-8")
            }&artist_name=${
                java.net.URLEncoder.encode(artist, "UTF-8")
            }"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CloudMusic/1.0.0")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val results = gson.fromJson(body, Array<LrcLibResult>::class.java)
                    if (results.isNotEmpty()) {
                        val lyric = results[0].syncedLyrics ?: results[0].plainLyrics ?: ""
                        if (lyric.isNotEmpty()) {
                            Log.d(TAG, "Found lyrics from LRCLIB")
                            return@withContext LyricModel(
                                songStatus = 1,
                                lyricVersion = 1,
                                lyric = lyric,
                                code = 200
                            )
                        }
                    }
                }
            }

            Log.d(TAG, "No lyrics found for: $title by $artist")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get lyrics", e)
            null
        }
    }

    /**
     * Clear stream URL cache
     */
    fun clearStreamUrlCache() {
        streamUrlCache.clear()
    }

    /**
     * Clear search pagination state
     */
    fun clearSearchState() {
        lastSearchContinuation = null
        lastSearchQuery = null
    }

    // LRCLIB response model
    private data class LrcLibResult(
        val id: Int?,
        val trackName: String?,
        val artistName: String?,
        val albumName: String?,
        val duration: Int?,
        val instrumental: Boolean?,
        val plainLyrics: String?,
        val syncedLyrics: String?
    )
}
