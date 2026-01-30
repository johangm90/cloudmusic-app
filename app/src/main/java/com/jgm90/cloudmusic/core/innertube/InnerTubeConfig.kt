package com.jgm90.cloudmusic.core.innertube

object InnerTubeConfig {
    const val BASE_URL = "https://youtubei.googleapis.com/youtubei/v1/"

    // API Keys for different clients
    object ApiKeys {
        const val WEB = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        const val ANDROID = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
        const val IOS = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc"
        const val YOUTUBE_MUSIC = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
    }

    // Client versions
    object ClientVersions {
        const val WEB = "2.20250115.01.00"
        const val WEB_REMIX = "1.20250113.01.00"  // YouTube Music Web
        const val ANDROID = "19.17.34"
        const val IOS = "19.16.3"
        const val YOUTUBE_MUSIC_ANDROID = "7.27.52"
    }

    // User agents
    object UserAgents {
        const val WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        const val ANDROID = "com.google.android.youtube/19.17.34 (Linux; U; Android 12; US) gzip"
        const val ANDROID_MUSIC = "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 12; US) gzip"
        const val IOS = "com.google.ios.youtube/19.16.3 (iPhone14,5; U; CPU iOS 17_5_1 like Mac OS X)"
    }

    // Client IDs
    object ClientIds {
        const val WEB = 1
        const val WEB_REMIX = 67  // YouTube Music Web
        const val ANDROID = 3
        const val IOS = 5
        const val YOUTUBE_MUSIC_ANDROID = 21
    }

    // Endpoints
    object Endpoints {
        const val CONFIG = "config"
        const val GUIDE = "guide"
        const val PLAYER = "player"
        const val BROWSE = "browse"
        const val SEARCH = "search"
        const val NEXT = "next"
        const val GET_TRANSCRIPT = "get_transcript"
        const val MUSIC_GET_SEARCH_SUGGESTIONS = "music/get_search_suggestions"
        const val MUSIC_GET_QUEUE = "music/get_queue"
    }

    // Search params for YouTube Music - filter songs only
    object SearchParams {
        // EgWKAQIIAWoKEAMQBBAJEAoQBQ== decoded: filter for songs
        const val SONGS_FILTER = "EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D"
        const val VIDEOS_FILTER = "EgWKAQIQAWoKEAMQBBAJEAoQBQ%3D%3D"
        const val ALBUMS_FILTER = "EgWKAQIYAWoKEAMQBBAJEAoQBQ%3D%3D"
        const val ARTISTS_FILTER = "EgWKAQIgAWoKEAMQBBAJEAoQBQ%3D%3D"
        const val PLAYLISTS_FILTER = "EgWKAQIoAWoKEAMQBBAJEAoQBQ%3D%3D"
    }

    // Browse IDs
    object BrowseIds {
        const val MUSIC_HOME = "FEmusic_home"
        const val MUSIC_EXPLORE = "FEmusic_explore"
        const val MUSIC_NEW_RELEASES = "FEmusic_new_releases_albums"
        const val MUSIC_CHARTS = "FEmusic_charts"
        const val MUSIC_MOODS_AND_GENRES = "FEmusic_moods_and_genres"
        const val WHAT_TO_WATCH = "FEwhat_to_watch"
        const val SHORTS = "FEshorts"
        const val LIBRARY = "FElibrary"
    }

    // Referrers
    object Referrers {
        const val YOUTUBE = "https://www.youtube.com/"
        const val YOUTUBE_MUSIC = "https://music.youtube.com/"
        const val YOUTUBE_MOBILE = "https://m.youtube.com/"
    }
}

enum class InnerTubeClient(
    val clientName: String,
    val clientVersion: String,
    val apiKey: String,
    val userAgent: String,
    val clientId: Int,
    val referer: String,
    val platform: String = "DESKTOP",
    val osVersion: String? = null,
    val loginSupported: Boolean = false,
    val useSignatureTimestamp: Boolean = false,
    val isEmbedded: Boolean = false,
    val baseUrl: String = InnerTubeConfig.BASE_URL
) {
    WEB(
        clientName = "WEB",
        clientVersion = InnerTubeConfig.ClientVersions.WEB,
        apiKey = InnerTubeConfig.ApiKeys.WEB,
        userAgent = InnerTubeConfig.UserAgents.WEB,
        clientId = InnerTubeConfig.ClientIds.WEB,
        referer = InnerTubeConfig.Referrers.YOUTUBE,
        baseUrl = "https://www.youtube.com/youtubei/v1/"
    ),
    // YouTube Music Web client (WEB_REMIX) - for search
    WEB_REMIX(
        clientName = "WEB_REMIX",
        clientVersion = InnerTubeConfig.ClientVersions.WEB_REMIX,
        apiKey = InnerTubeConfig.ApiKeys.YOUTUBE_MUSIC,
        userAgent = InnerTubeConfig.UserAgents.WEB,
        clientId = InnerTubeConfig.ClientIds.WEB_REMIX,
        referer = InnerTubeConfig.Referrers.YOUTUBE_MUSIC,
        loginSupported = true,
        useSignatureTimestamp = true,
        baseUrl = "https://music.youtube.com/youtubei/v1/"
    ),
    ANDROID(
        clientName = "ANDROID",
        clientVersion = InnerTubeConfig.ClientVersions.ANDROID,
        apiKey = InnerTubeConfig.ApiKeys.ANDROID,
        userAgent = InnerTubeConfig.UserAgents.ANDROID,
        clientId = InnerTubeConfig.ClientIds.ANDROID,
        referer = InnerTubeConfig.Referrers.YOUTUBE,
        platform = "MOBILE",
        osVersion = "12",
        loginSupported = true,
        useSignatureTimestamp = true,
        baseUrl = "https://www.youtube.com/youtubei/v1/"
    ),
    IOS(
        clientName = "IOS",
        clientVersion = InnerTubeConfig.ClientVersions.IOS,
        apiKey = InnerTubeConfig.ApiKeys.IOS,
        userAgent = InnerTubeConfig.UserAgents.IOS,
        clientId = InnerTubeConfig.ClientIds.IOS,
        referer = InnerTubeConfig.Referrers.YOUTUBE,
        platform = "MOBILE",
        osVersion = "17.5.1",
        baseUrl = "https://www.youtube.com/youtubei/v1/"
    ),
    YOUTUBE_MUSIC_ANDROID(
        clientName = "ANDROID_MUSIC",
        clientVersion = InnerTubeConfig.ClientVersions.YOUTUBE_MUSIC_ANDROID,
        apiKey = InnerTubeConfig.ApiKeys.YOUTUBE_MUSIC,
        userAgent = InnerTubeConfig.UserAgents.ANDROID_MUSIC,
        clientId = InnerTubeConfig.ClientIds.YOUTUBE_MUSIC_ANDROID,
        referer = InnerTubeConfig.Referrers.YOUTUBE_MUSIC,
        platform = "MOBILE",
        osVersion = "12",
        loginSupported = true,
        useSignatureTimestamp = true,
        baseUrl = "https://music.youtube.com/youtubei/v1/"
    );

    fun buildHeaders(): Map<String, String> = mapOf(
        "User-Agent" to userAgent,
        "Referer" to referer,
        "X-Origin" to referer.trimEnd('/'),
        "Origin" to referer.trimEnd('/'),
        "Content-Type" to "application/json",
        "X-Goog-Api-Key" to apiKey,
        "X-Goog-Api-Format-Version" to "1",
        "X-YouTube-Client-Name" to clientId.toString(),
        "X-YouTube-Client-Version" to clientVersion
    )
}
