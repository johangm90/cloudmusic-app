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
import java.net.Proxy
import java.util.Locale

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

    var locale: InnerTubeLocale = InnerTubeLocale(
        gl = Locale.getDefault().country.ifBlank { "US" },
        hl = Locale.getDefault().toLanguageTag().ifBlank { "en" }
    )
    var visitorData: String? = null
    var dataSyncId: String? = null
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value.isNullOrBlank()) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    var proxy: Proxy? = null
    var useLoginForBrowse: Boolean = false

    private fun buildUrl(endpoint: String, clientType: InnerTubeClient): String {
        return "${clientType.baseUrl}$endpoint?key=${clientType.apiKey}&prettyPrint=false"
    }

    private fun buildContext(
        clientType: InnerTubeClient,
        includeLogin: Boolean = false,
        thirdParty: ThirdPartyContext? = null
    ): InnerTubeContext {
        return InnerTubeContext(
            client = ClientContext(
                clientName = clientType.clientName,
                clientVersion = clientType.clientVersion,
                platform = clientType.platform,
                hl = locale.hl,
                gl = locale.gl,
                visitorData = visitorData,
                osVersion = clientType.osVersion
            ),
            thirdParty = thirdParty,
            user = UserContext(
                onBehalfOfUser = if (includeLogin && clientType.loginSupported) dataSyncId else null
            )
        )
    }

    private suspend fun <T> executeRequest(
        endpoint: String,
        body: Any,
        responseClass: Class<T>,
        clientType: InnerTubeClient,
        includeLogin: Boolean = false,
        continuation: String? = null
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
            if (includeLogin && clientType.loginSupported) {
                cookie?.let { cookie ->
                    requestBuilder.addHeader("Cookie", cookie)
                    val sapisid = cookieMap["SAPISID"]
                    if (!sapisid.isNullOrBlank()) {
                        val currentTime = System.currentTimeMillis() / 1000
                        val sapisidHash = sha1("$currentTime $sapisid ${clientType.referer.trimEnd('/')}")
                        requestBuilder.addHeader("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                    }
                }
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
            context = buildContext(InnerTubeClient.WEB_REMIX, includeLogin = useLoginForBrowse),
            query = query,
            params = params,
            continuation = continuation
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.SEARCH,
            body,
            SearchResponse::class.java,
            InnerTubeClient.WEB_REMIX,
            includeLogin = useLoginForBrowse,
            continuation = continuation
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
            InnerTubeClient.WEB,
            continuation = continuation
        )
    }

    /**
     * Get player info using ANDROID client (best for getting direct stream URLs)
     */
    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        signatureTimestamp: Int? = null,
        clientType: InnerTubeClient = InnerTubeClient.ANDROID
    ): Result<PlayerResponse> {
        val thirdParty = if (clientType.isEmbedded) {
            ThirdPartyContext(embedUrl = "https://www.youtube.com/watch?v=$videoId")
        } else {
            null
        }
        val body = InnerTubeRequest(
            context = buildContext(clientType, includeLogin = true, thirdParty = thirdParty),
            videoId = videoId,
            playlistId = playlistId,
            playbackContext = if (clientType.useSignatureTimestamp && signatureTimestamp != null) {
                PlaybackContext(PlaybackContext.ContentPlaybackContext(signatureTimestamp))
            } else null
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.PLAYER,
            body,
            PlayerResponse::class.java,
            clientType,
            includeLogin = true
        )
    }

    /**
     * Browse content (YouTube Music)
     */
    suspend fun browse(
        browseId: String,
        params: String? = null,
        continuation: String? = null,
        clientType: InnerTubeClient = InnerTubeClient.WEB_REMIX,
        includeLogin: Boolean = false
    ): Result<BrowseResponse> {
        val body = InnerTubeRequest(
            context = buildContext(
                clientType,
                includeLogin = includeLogin || useLoginForBrowse
            ),
            browseId = browseId,
            params = params,
            continuation = continuation
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.BROWSE,
            body,
            BrowseResponse::class.java,
            clientType,
            includeLogin = includeLogin || useLoginForBrowse,
            continuation = continuation
        )
    }

    /**
     * Get next/related content
     */
    suspend fun next(
        videoId: String,
        playlistId: String? = null,
        playlistSetVideoId: String? = null,
        index: Int? = null,
        params: String? = null,
        continuation: String? = null,
        clientType: InnerTubeClient = InnerTubeClient.WEB_REMIX
    ): Result<NextResponse> {
        val body = InnerTubeRequest(
            context = buildContext(clientType, includeLogin = true),
            videoId = videoId,
            playlistId = playlistId,
            playlistSetVideoId = playlistSetVideoId,
            index = index,
            params = params,
            continuation = continuation
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.NEXT,
            body,
            NextResponse::class.java,
            clientType,
            includeLogin = true,
            continuation = continuation
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
    suspend fun musicGetQueue(
        videoIds: List<String>,
        playlistId: String? = null
    ): Result<MusicQueueResponse> {
        val body = mapOf(
            "context" to buildContext(InnerTubeClient.WEB_REMIX),
            "videoIds" to videoIds,
            "playlistId" to playlistId
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.MUSIC_GET_QUEUE,
            body,
            MusicQueueResponse::class.java,
            InnerTubeClient.WEB_REMIX
        )
    }

    suspend fun getTranscript(
        videoId: String,
        clientType: InnerTubeClient = InnerTubeClient.WEB_REMIX
    ): Result<Any> {
        val body = mapOf(
            "context" to buildContext(clientType),
            "params" to ("\n${11.toChar()}$videoId").encodeBase64()
        )
        return executeRequest(
            InnerTubeConfig.Endpoints.GET_TRANSCRIPT,
            body,
            Any::class.java,
            clientType
        )
    }

    suspend fun registerPlayback(
        url: String,
        cpn: String,
        playlistId: String? = null,
        clientType: InnerTubeClient = InnerTubeClient.WEB_REMIX
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            clientType.buildHeaders().forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
            requestBuilder.addHeader("X-Goog-Api-Format-Version", "1")

            val urlBuilder = requestBuilder.build().url.newBuilder()
                .addQueryParameter("ver", "2")
                .addQueryParameter("c", clientType.clientName)
                .addQueryParameter("cpn", cpn)

            if (playlistId != null) {
                urlBuilder.addQueryParameter("list", playlistId)
                urlBuilder.addQueryParameter("referrer", "https://music.youtube.com/playlist?list=$playlistId")
            }

            val request = requestBuilder.url(urlBuilder.build()).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("HTTP ${response.code}: ${response.message}")
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
