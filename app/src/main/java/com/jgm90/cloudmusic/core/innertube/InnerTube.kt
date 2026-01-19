package com.jgm90.cloudmusic.core.innertube

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jgm90.cloudmusic.core.innertube.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class InnerTube(
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "InnerTube"
    }

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildUrl(endpoint: String, clientType: InnerTubeClient): String {
        return "${InnerTubeConfig.BASE_URL}$endpoint?key=${clientType.apiKey}&prettyPrint=false"
    }

    private fun buildContext(clientType: InnerTubeClient): InnerTubeContext {
        return InnerTubeContext(
            client = ClientContext(
                clientName = clientType.clientName,
                clientVersion = clientType.clientVersion,
                platform = clientType.platform,
                hl = "en",
                gl = "US"
            )
        )
    }

    private suspend fun <T> executeRequest(
        endpoint: String,
        body: Any,
        responseClass: Class<T>,
        clientType: InnerTubeClient
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(endpoint, clientType)
            val jsonBody = gson.toJson(body)

            Log.d(TAG, "Request to $endpoint with ${clientType.clientName}: $jsonBody")

            val requestBuilder = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(jsonMediaType))

            clientType.buildHeaders().forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e(TAG, "Error response: $errorBody")
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}: ${response.message}")
                )
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                return@withContext Result.failure(IOException("Empty response body"))
            }

            Log.d(TAG, "Response from $endpoint: ${responseBody.take(1000)}...")

            val result = gson.fromJson(responseBody, responseClass)
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            Result.failure(e)
        }
    }

    /**
     * Search using YouTube Music (WEB_REMIX client)
     * Returns music-specific results with album art
     */
    suspend fun searchMusic(
        query: String,
        params: String? = InnerTubeConfig.SearchParams.SONGS_FILTER,
        continuation: String? = null
    ): Result<SearchResponse> {
        val body = InnerTubeRequest(
            context = buildContext(InnerTubeClient.WEB_REMIX),
            query = query,
            params = params,
            continuation = continuation
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.SEARCH,
            body,
            SearchResponse::class.java,
            InnerTubeClient.WEB_REMIX
        )
    }

    /**
     * Search using standard YouTube (WEB client)
     */
    suspend fun search(
        query: String,
        params: String? = null,
        continuation: String? = null
    ): Result<SearchResponse> {
        val body = InnerTubeRequest(
            context = buildContext(InnerTubeClient.WEB),
            query = query,
            params = params,
            continuation = continuation
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.SEARCH,
            body,
            SearchResponse::class.java,
            InnerTubeClient.WEB
        )
    }

    /**
     * Get player info using ANDROID client (best for getting direct stream URLs)
     */
    suspend fun player(videoId: String): Result<PlayerResponse> {
        val body = InnerTubeRequest(
            context = buildContext(InnerTubeClient.ANDROID),
            videoId = videoId
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.PLAYER,
            body,
            PlayerResponse::class.java,
            InnerTubeClient.ANDROID
        )
    }

    /**
     * Browse content (YouTube Music)
     */
    suspend fun browse(
        browseId: String,
        params: String? = null,
        continuation: String? = null
    ): Result<BrowseResponse> {
        val body = InnerTubeRequest(
            context = buildContext(InnerTubeClient.WEB_REMIX),
            browseId = browseId,
            params = params,
            continuation = continuation
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.BROWSE,
            body,
            BrowseResponse::class.java,
            InnerTubeClient.WEB_REMIX
        )
    }

    /**
     * Get next/related content
     */
    suspend fun next(
        videoId: String,
        playlistId: String? = null
    ): Result<NextResponse> {
        val body = InnerTubeRequest(
            context = buildContext(InnerTubeClient.WEB_REMIX),
            videoId = videoId,
            playlistId = playlistId
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.NEXT,
            body,
            NextResponse::class.java,
            InnerTubeClient.WEB_REMIX
        )
    }

    /**
     * Get music search suggestions (YouTube Music)
     */
    suspend fun musicSearchSuggestions(query: String): Result<MusicSearchSuggestionsResponse> {
        val body = mapOf(
            "context" to buildContext(InnerTubeClient.WEB_REMIX),
            "input" to query
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.MUSIC_GET_SEARCH_SUGGESTIONS,
            body,
            MusicSearchSuggestionsResponse::class.java,
            InnerTubeClient.WEB_REMIX
        )
    }

    /**
     * Get music queue (related songs)
     */
    suspend fun musicGetQueue(videoIds: List<String>): Result<MusicQueueResponse> {
        val body = mapOf(
            "context" to buildContext(InnerTubeClient.WEB_REMIX),
            "videoIds" to videoIds
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.MUSIC_GET_QUEUE,
            body,
            MusicQueueResponse::class.java,
            InnerTubeClient.WEB_REMIX
        )
    }
}
