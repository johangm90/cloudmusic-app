package com.jgm90.cloudmusic.core.innertube.models

import com.google.gson.annotations.SerializedName

// ============== Request Models ==============

data class InnerTubeRequest(
    val context: InnerTubeContext,
    val query: String? = null,
    val videoId: String? = null,
    val browseId: String? = null,
    val params: String? = null,
    val continuation: String? = null,
    val playlistId: String? = null,
    val playlistSetVideoId: String? = null,
    val index: Int? = null,
    val playbackContext: PlaybackContext? = null
)

data class PlaybackContext(
    val contentPlaybackContext: ContentPlaybackContext
) {
    data class ContentPlaybackContext(
        val signatureTimestamp: Int
    )
}

data class InnerTubeContext(
    val client: ClientContext,
    val thirdParty: ThirdPartyContext? = null,
    val request: RequestContext = RequestContext(),
    val user: UserContext = UserContext()
)

data class ClientContext(
    val clientName: String,
    val clientVersion: String,
    val platform: String = "DESKTOP",
    val hl: String = "en",
    val gl: String = "US",
    val visitorData: String? = null,
    val userAgent: String? = null,
    val deviceModel: String? = null,
    val androidSdkVersion: Int? = null,
    val osName: String? = null,
    val osVersion: String? = null
)

data class ThirdPartyContext(
    val embedUrl: String
)

data class RequestContext(
    val internalExperimentFlags: List<String> = emptyList(),
    val useSsl: Boolean = true
)

data class UserContext(
    val lockedSafetyMode: Boolean = false,
    val onBehalfOfUser: String? = null
)

// ============== Response Models ==============

data class InnerTubeError(
    val code: Int,
    val message: String,
    val status: String? = null,
    val errors: List<ErrorDetail>? = null
) {
    data class ErrorDetail(
        val message: String?,
        val domain: String?,
        val reason: String?
    )
}

// ============== Search Response ==============

data class SearchResponse(
    val contents: SearchContents?,
    val continuationContents: ContinuationContents?,
    val estimatedResults: String?,
    val responseContext: ResponseContext?
)

data class SearchContents(
    val twoColumnSearchResultsRenderer: TwoColumnSearchResultsRenderer?,
    val sectionListRenderer: SectionListRenderer?,
    val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer?
)

data class TwoColumnSearchResultsRenderer(
    val primaryContents: PrimaryContents?
)

data class PrimaryContents(
    val sectionListRenderer: SectionListRenderer?
)

data class TabbedSearchResultsRenderer(
    val tabs: List<Tab>?
)

data class Tab(
    val tabRenderer: TabRenderer?
)

data class TabRenderer(
    val content: TabContent?
)

data class TabContent(
    val sectionListRenderer: SectionListRenderer?
)

data class SectionListRenderer(
    val contents: List<SectionContent>?,
    val continuations: List<Continuation>?
)

data class SectionContent(
    val itemSectionRenderer: ItemSectionRenderer?,
    val musicShelfRenderer: MusicShelfRenderer?,
    val musicCardShelfRenderer: MusicCardShelfRenderer?
)

data class ItemSectionRenderer(
    val contents: List<ItemContent>?
)

data class ItemContent(
    val videoRenderer: VideoRenderer?,
    val playlistRenderer: PlaylistRenderer?,
    val channelRenderer: ChannelRenderer?,
    val compactVideoRenderer: CompactVideoRenderer?
)

// ============== Music Shelf (YouTube Music) ==============

data class MusicShelfRenderer(
    val title: TextRuns?,
    val contents: List<MusicShelfContent>?,
    val continuations: List<Continuation>?,
    val bottomEndpoint: NavigationEndpoint?
)

data class MusicShelfContent(
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?
)

data class MusicResponsiveListItemRenderer(
    val flexColumns: List<FlexColumn>?,
    val thumbnail: MusicThumbnailRenderer?,
    val playlistItemData: PlaylistItemData?,
    val navigationEndpoint: NavigationEndpoint?,
    val overlay: MusicItemOverlay?
)

data class FlexColumn(
    val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer?
)

data class MusicResponsiveListItemFlexColumnRenderer(
    val text: TextRuns?
)

data class MusicTwoRowItemRenderer(
    val thumbnailRenderer: MusicThumbnailRenderer?,
    val title: TextRuns?,
    val subtitle: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

data class MusicThumbnailRenderer(
    val musicThumbnailRenderer: ThumbnailContent?
)

data class ThumbnailContent(
    val thumbnail: Thumbnails?
)

data class MusicItemOverlay(
    val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer?
)

data class MusicItemThumbnailOverlayRenderer(
    val content: MusicPlayButtonRenderer?
)

data class MusicPlayButtonRenderer(
    val musicPlayButtonRenderer: PlayButtonContent?
)

data class PlayButtonContent(
    val playNavigationEndpoint: NavigationEndpoint?
)

data class PlaylistItemData(
    val videoId: String?,
    val playlistSetVideoId: String?
)

data class MusicCardShelfRenderer(
    val title: TextRuns?,
    val subtitle: TextRuns?,
    val thumbnail: MusicThumbnailRenderer?,
    val onTap: NavigationEndpoint?
)

// ============== Video Renderer ==============

data class VideoRenderer(
    val videoId: String?,
    val thumbnail: Thumbnails?,
    val title: TextRuns?,
    val ownerText: TextRuns?,
    val shortBylineText: TextRuns?,
    val lengthText: SimpleText?,
    val viewCountText: SimpleText?,
    val publishedTimeText: SimpleText?,
    val descriptionSnippet: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

data class CompactVideoRenderer(
    val videoId: String?,
    val thumbnail: Thumbnails?,
    val title: SimpleText?,
    val shortBylineText: TextRuns?,
    val lengthText: SimpleText?,
    val viewCountText: SimpleText?
)

data class PlaylistRenderer(
    val playlistId: String?,
    val title: SimpleText?,
    val thumbnails: List<Thumbnails>?,
    val videoCount: String?,
    val shortBylineText: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

data class ChannelRenderer(
    val channelId: String?,
    val title: SimpleText?,
    val thumbnail: Thumbnails?,
    val subscriberCountText: SimpleText?,
    val descriptionSnippet: TextRuns?
)

// ============== Common Components ==============

data class Thumbnails(
    val thumbnails: List<Thumbnail>?
)

data class Thumbnail(
    val url: String?,
    val width: Int?,
    val height: Int?
)

data class TextRuns(
    val runs: List<TextRun>?,
    val simpleText: String?,
    val accessibility: Accessibility?
) {
    fun getText(): String = runs?.joinToString("") { it.text ?: "" } ?: simpleText ?: ""
}

data class TextRun(
    val text: String?,
    val navigationEndpoint: NavigationEndpoint?
)

data class SimpleText(
    val simpleText: String?,
    val runs: List<TextRun>?,
    val accessibility: Accessibility?
) {
    fun getText(): String = simpleText ?: runs?.joinToString("") { it.text ?: "" } ?: ""
}

data class Accessibility(
    val accessibilityData: AccessibilityData?
)

data class AccessibilityData(
    val label: String?
)

data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint?,
    val browseEndpoint: BrowseEndpoint?,
    val searchEndpoint: SearchEndpoint?
)

data class WatchEndpoint(
    val videoId: String?,
    val playlistId: String?,
    val index: Int?,
    val params: String?,
    val watchEndpointMusicSupportedConfigs: WatchEndpointMusicSupportedConfigs?
)

data class WatchEndpointMusicSupportedConfigs(
    val watchEndpointMusicConfig: WatchEndpointMusicConfig?
)

data class WatchEndpointMusicConfig(
    val musicVideoType: String?
)

data class BrowseEndpoint(
    val browseId: String?,
    val params: String?,
    val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs?
)

data class BrowseEndpointContextSupportedConfigs(
    val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig?
)

data class BrowseEndpointContextMusicConfig(
    val pageType: String?
)

data class SearchEndpoint(
    val query: String?,
    val params: String?
)

data class Continuation(
    val nextContinuationData: ContinuationData?,
    val reloadContinuationData: ContinuationData?
)

data class ContinuationData(
    val continuation: String?,
    val clickTrackingParams: String?
)

data class ContinuationContents(
    val sectionListContinuation: SectionListRenderer?,
    val musicShelfContinuation: MusicShelfRenderer?
)

data class ResponseContext(
    val visitorData: String?,
    val serviceTrackingParams: List<ServiceTrackingParam>?
)

data class ServiceTrackingParam(
    val service: String?,
    val params: List<Param>?
)

data class Param(
    val key: String?,
    val value: String?
)

// ============== Player Response ==============

data class PlayerResponse(
    val videoDetails: VideoDetails?,
    val streamingData: StreamingData?,
    val playabilityStatus: PlayabilityStatus?,
    val responseContext: ResponseContext?
)

data class VideoDetails(
    val videoId: String?,
    val title: String?,
    val lengthSeconds: String?,
    val keywords: List<String>?,
    val channelId: String?,
    val shortDescription: String?,
    val thumbnail: Thumbnails?,
    val viewCount: String?,
    val author: String?,
    val isLiveContent: Boolean?,
    val isPrivate: Boolean?,
    val musicVideoType: String?
)

data class StreamingData(
    val expiresInSeconds: String?,
    val formats: List<Format>?,
    val adaptiveFormats: List<Format>?,
    val hlsManifestUrl: String?,
    val dashManifestUrl: String?
)

data class Format(
    val itag: Int?,
    val url: String?,
    val mimeType: String?,
    val bitrate: Int?,
    val width: Int?,
    val height: Int?,
    val contentLength: String?,
    val quality: String?,
    val qualityLabel: String?,
    val audioQuality: String?,
    val audioSampleRate: String?,
    val audioChannels: Int?,
    val averageBitrate: Int?,
    val approxDurationMs: String?,
    val signatureCipher: String?,
    @SerializedName("cipher")
    val cipher: String?
) {
    fun isAudioOnly(): Boolean = height == null && audioQuality != null
    fun isVideoOnly(): Boolean = audioQuality == null && height != null
    fun hasAudioAndVideo(): Boolean = audioQuality != null && height != null
}

data class PlayabilityStatus(
    val status: String?,
    val playableInEmbed: Boolean?,
    val reason: String?,
    val errorScreen: ErrorScreen?,
    val liveStreamability: LiveStreamability?
) {
    fun isPlayable(): Boolean = status == "OK"
}

data class ErrorScreen(
    val playerErrorMessageRenderer: PlayerErrorMessageRenderer?
)

data class PlayerErrorMessageRenderer(
    val reason: SimpleText?,
    val subreason: SimpleText?
)

data class LiveStreamability(
    val liveStreamabilityRenderer: LiveStreamabilityRenderer?
)

data class LiveStreamabilityRenderer(
    val videoId: String?,
    val broadcastId: String?
)

// ============== Browse Response ==============

data class BrowseResponse(
    val contents: BrowseContents?,
    val header: BrowseHeader?,
    val continuationContents: ContinuationContents?,
    val responseContext: ResponseContext?
)

data class BrowseContents(
    val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer?,
    val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?,
    val sectionListRenderer: SectionListRenderer?
)

data class SingleColumnBrowseResultsRenderer(
    val tabs: List<Tab>?
)

data class TwoColumnBrowseResultsRenderer(
    val tabs: List<Tab>?,
    val secondaryContents: SecondaryContents?
)

data class SecondaryContents(
    val sectionListRenderer: SectionListRenderer?
)

data class BrowseHeader(
    val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
    val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?
)

data class MusicImmersiveHeaderRenderer(
    val title: TextRuns?,
    val description: TextRuns?,
    val thumbnail: MusicThumbnailRenderer?,
    val playButton: PlayButton?,
    val startRadioButton: StartRadioButton?,
    val menu: Menu?
)

data class MusicDetailHeaderRenderer(
    val title: TextRuns?,
    val subtitle: TextRuns?,
    val thumbnail: MusicThumbnailRenderer?,
    val menu: Menu?,
    val secondSubtitle: TextRuns?
)

data class PlayButton(
    val buttonRenderer: ButtonRenderer?
)

data class StartRadioButton(
    val buttonRenderer: ButtonRenderer?
)

data class ButtonRenderer(
    val navigationEndpoint: NavigationEndpoint?,
    val text: TextRuns?
)

data class Menu(
    val menuRenderer: MenuRenderer?
)

data class MenuRenderer(
    val items: List<MenuItem>?
)

data class MenuItem(
    val menuNavigationItemRenderer: MenuNavigationItemRenderer?,
    val menuServiceItemRenderer: MenuServiceItemRenderer?
)

data class MenuNavigationItemRenderer(
    val text: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

data class MenuServiceItemRenderer(
    val text: TextRuns?,
    val serviceEndpoint: ServiceEndpoint?
)

data class ServiceEndpoint(
    val playlistEditEndpoint: PlaylistEditEndpoint?
)

data class PlaylistEditEndpoint(
    val playlistId: String?,
    val actions: List<PlaylistEditAction>?
)

data class PlaylistEditAction(
    val setVideoId: String?,
    val removedVideoId: String?,
    val addedVideoId: String?
)

// ============== Next Response (Related/Suggestions) ==============

data class NextResponse(
    val contents: NextContents?,
    val currentVideoEndpoint: NavigationEndpoint?,
    val responseContext: ResponseContext?
)

data class NextContents(
    val twoColumnWatchNextResults: TwoColumnWatchNextResults?,
    val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?
)

data class TwoColumnWatchNextResults(
    val results: WatchNextResults?,
    val secondaryResults: SecondaryResults?
)

data class WatchNextResults(
    val results: ResultsContents?
)

data class ResultsContents(
    val contents: List<WatchNextContent>?
)

data class WatchNextContent(
    val videoPrimaryInfoRenderer: VideoPrimaryInfoRenderer?,
    val videoSecondaryInfoRenderer: VideoSecondaryInfoRenderer?
)

data class VideoPrimaryInfoRenderer(
    val title: TextRuns?,
    val viewCount: ViewCount?,
    val dateText: SimpleText?
)

data class ViewCount(
    val videoViewCountRenderer: VideoViewCountRenderer?
)

data class VideoViewCountRenderer(
    val viewCount: SimpleText?,
    val shortViewCount: SimpleText?
)

data class VideoSecondaryInfoRenderer(
    val owner: Owner?,
    val description: TextRuns?
)

data class Owner(
    val videoOwnerRenderer: VideoOwnerRenderer?
)

data class VideoOwnerRenderer(
    val title: TextRuns?,
    val thumbnail: Thumbnails?,
    val subscriberCountText: SimpleText?
)

data class SecondaryResults(
    val secondaryResults: SecondaryResultsContent?
)

data class SecondaryResultsContent(
    val results: List<SecondaryResultItem>?
)

data class SecondaryResultItem(
    val compactVideoRenderer: CompactVideoRenderer?,
    val compactPlaylistRenderer: CompactPlaylistRenderer?
)

data class CompactPlaylistRenderer(
    val playlistId: String?,
    val title: SimpleText?,
    val thumbnail: Thumbnails?,
    val videoCountText: TextRuns?
)

data class SingleColumnMusicWatchNextResultsRenderer(
    val tabbedRenderer: TabbedRenderer?
)

data class TabbedRenderer(
    val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?
)

data class WatchNextTabbedResultsRenderer(
    val tabs: List<Tab>?
)

// ============== Music Search Suggestions ==============

data class MusicSearchSuggestionsResponse(
    val contents: List<MusicSearchSuggestionContent>?,
    val responseContext: ResponseContext?
)

data class MusicSearchSuggestionContent(
    val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer?
)

data class SearchSuggestionsSectionRenderer(
    val contents: List<SearchSuggestionItem>?
)

data class SearchSuggestionItem(
    val searchSuggestionRenderer: SearchSuggestionRenderer?
)

data class SearchSuggestionRenderer(
    val suggestion: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

// ============== Music Queue ==============

data class MusicQueueResponse(
    val queueDatas: List<QueueData>?,
    val responseContext: ResponseContext?
)

data class QueueData(
    val content: QueueContent?
)

data class QueueContent(
    val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?
)

data class PlaylistPanelVideoRenderer(
    val title: TextRuns?,
    val longBylineText: TextRuns?,
    val thumbnail: Thumbnails?,
    val lengthText: SimpleText?,
    val videoId: String?,
    val navigationEndpoint: NavigationEndpoint?
)
