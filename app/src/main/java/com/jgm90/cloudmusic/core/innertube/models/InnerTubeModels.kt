package com.jgm90.cloudmusic.core.innertube.models

import kotlinx.serialization.Serializable

// ============== Request Models ==============

@Serializable
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

@Serializable
data class PlaybackContext(
    val contentPlaybackContext: ContentPlaybackContext
) {
    @Serializable
    data class ContentPlaybackContext(
        val signatureTimestamp: Int
    )
}

@Serializable
data class InnerTubeContext(
    val client: ClientContext,
    val thirdParty: ThirdPartyContext? = null,
    val request: RequestContext = RequestContext(),
    val user: UserContext = UserContext()
)

@Serializable
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

@Serializable
data class ThirdPartyContext(
    val embedUrl: String
)

@Serializable
data class RequestContext(
    val internalExperimentFlags: List<String> = emptyList(),
    val useSsl: Boolean = true
)

@Serializable
data class UserContext(
    val lockedSafetyMode: Boolean = false,
    val onBehalfOfUser: String? = null
)

// ============== Response Models ==============

@Serializable
data class InnerTubeError(
    val code: Int,
    val message: String,
    val status: String? = null,
    val errors: List<ErrorDetail>? = null
) {
    @Serializable
    data class ErrorDetail(
        val message: String?,
        val domain: String?,
        val reason: String?
    )
}

// ============== Search Response ==============

@Serializable
data class SearchResponse(
    val contents: SearchContents?,
    val continuationContents: ContinuationContents?,
    val estimatedResults: String?,
    val responseContext: ResponseContext?
)

@Serializable
data class SearchContents(
    val twoColumnSearchResultsRenderer: TwoColumnSearchResultsRenderer?,
    val sectionListRenderer: SectionListRenderer?,
    val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer?
)

@Serializable
data class TwoColumnSearchResultsRenderer(
    val primaryContents: PrimaryContents?
)

@Serializable
data class PrimaryContents(
    val sectionListRenderer: SectionListRenderer?
)

@Serializable
data class TabbedSearchResultsRenderer(
    val tabs: List<Tab>?
)

@Serializable
data class Tab(
    val tabRenderer: TabRenderer?
)

@Serializable
data class TabRenderer(
    val content: TabContent?
)

@Serializable
data class TabContent(
    val sectionListRenderer: SectionListRenderer?
)

@Serializable
data class SectionListRenderer(
    val contents: List<SectionContent>?,
    val continuations: List<Continuation>?
)

@Serializable
data class SectionContent(
    val itemSectionRenderer: ItemSectionRenderer?,
    val musicShelfRenderer: MusicShelfRenderer?,
    val musicCardShelfRenderer: MusicCardShelfRenderer?
)

@Serializable
data class ItemSectionRenderer(
    val contents: List<ItemContent>?
)

@Serializable
data class ItemContent(
    val videoRenderer: VideoRenderer?,
    val playlistRenderer: PlaylistRenderer?,
    val channelRenderer: ChannelRenderer?,
    val compactVideoRenderer: CompactVideoRenderer?
)

// ============== Music Shelf (YouTube Music) ==============

@Serializable
data class MusicShelfRenderer(
    val title: TextRuns?,
    val contents: List<MusicShelfContent>?,
    val continuations: List<Continuation>?,
    val bottomEndpoint: NavigationEndpoint?
)

@Serializable
data class MusicShelfContent(
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?
)

@Serializable
data class MusicResponsiveListItemRenderer(
    val flexColumns: List<FlexColumn>?,
    val thumbnail: MusicThumbnailRenderer?,
    val playlistItemData: PlaylistItemData?,
    val navigationEndpoint: NavigationEndpoint?,
    val overlay: MusicItemOverlay?
)

@Serializable
data class FlexColumn(
    val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer?
)

@Serializable
data class MusicResponsiveListItemFlexColumnRenderer(
    val text: TextRuns?
)

@Serializable
data class MusicTwoRowItemRenderer(
    val thumbnailRenderer: MusicThumbnailRenderer?,
    val title: TextRuns?,
    val subtitle: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

@Serializable
data class MusicThumbnailRenderer(
    val musicThumbnailRenderer: ThumbnailContent?
)

@Serializable
data class ThumbnailContent(
    val thumbnail: Thumbnails?
)

@Serializable
data class MusicItemOverlay(
    val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer?
)

@Serializable
data class MusicItemThumbnailOverlayRenderer(
    val content: MusicPlayButtonRenderer?
)

@Serializable
data class MusicPlayButtonRenderer(
    val musicPlayButtonRenderer: PlayButtonContent?
)

@Serializable
data class PlayButtonContent(
    val playNavigationEndpoint: NavigationEndpoint?
)

@Serializable
data class PlaylistItemData(
    val videoId: String?,
    val playlistSetVideoId: String?
)

@Serializable
data class MusicCardShelfRenderer(
    val title: TextRuns?,
    val subtitle: TextRuns?,
    val thumbnail: MusicThumbnailRenderer?,
    val onTap: NavigationEndpoint?
)

// ============== Video Renderer ==============

@Serializable
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

@Serializable
data class CompactVideoRenderer(
    val videoId: String?,
    val thumbnail: Thumbnails?,
    val title: SimpleText?,
    val shortBylineText: TextRuns?,
    val lengthText: SimpleText?,
    val viewCountText: SimpleText?
)

@Serializable
data class PlaylistRenderer(
    val playlistId: String?,
    val title: SimpleText?,
    val thumbnails: List<Thumbnails>?,
    val videoCount: String?,
    val shortBylineText: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

@Serializable
data class ChannelRenderer(
    val channelId: String?,
    val title: SimpleText?,
    val thumbnail: Thumbnails?,
    val subscriberCountText: SimpleText?,
    val descriptionSnippet: TextRuns?
)

// ============== Common Components ==============

@Serializable
data class Thumbnails(
    val thumbnails: List<Thumbnail>?
)

@Serializable
data class Thumbnail(
    val url: String?,
    val width: Int?,
    val height: Int?
)

@Serializable
data class TextRuns(
    val runs: List<TextRun>?,
    val simpleText: String?,
    val accessibility: Accessibility?
) {
    fun getText(): String = runs?.joinToString("") { it.text ?: "" } ?: simpleText ?: ""
}

@Serializable
data class TextRun(
    val text: String?,
    val navigationEndpoint: NavigationEndpoint?
)

@Serializable
data class SimpleText(
    val simpleText: String?,
    val runs: List<TextRun>?,
    val accessibility: Accessibility?
) {
    fun getText(): String = simpleText ?: runs?.joinToString("") { it.text ?: "" } ?: ""
}

@Serializable
data class Accessibility(
    val accessibilityData: AccessibilityData?
)

@Serializable
data class AccessibilityData(
    val label: String?
)

@Serializable
data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint?,
    val browseEndpoint: BrowseEndpoint?,
    val searchEndpoint: SearchEndpoint?
)

@Serializable
data class WatchEndpoint(
    val videoId: String?,
    val playlistId: String?,
    val index: Int?,
    val params: String?,
    val watchEndpointMusicSupportedConfigs: WatchEndpointMusicSupportedConfigs?
)

@Serializable
data class WatchEndpointMusicSupportedConfigs(
    val watchEndpointMusicConfig: WatchEndpointMusicConfig?
)

@Serializable
data class WatchEndpointMusicConfig(
    val musicVideoType: String?
)

@Serializable
data class BrowseEndpoint(
    val browseId: String?,
    val params: String?,
    val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs?
)

@Serializable
data class BrowseEndpointContextSupportedConfigs(
    val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig?
)

@Serializable
data class BrowseEndpointContextMusicConfig(
    val pageType: String?
)

@Serializable
data class SearchEndpoint(
    val query: String?,
    val params: String?
)

@Serializable
data class Continuation(
    val nextContinuationData: ContinuationData?,
    val reloadContinuationData: ContinuationData?
)

@Serializable
data class ContinuationData(
    val continuation: String?,
    val clickTrackingParams: String?
)

@Serializable
data class ContinuationContents(
    val sectionListContinuation: SectionListRenderer?,
    val musicShelfContinuation: MusicShelfRenderer?
)

@Serializable
data class ResponseContext(
    val visitorData: String?,
    val serviceTrackingParams: List<ServiceTrackingParam>?
)

@Serializable
data class ServiceTrackingParam(
    val service: String?,
    val params: List<Param>?
)

@Serializable
data class Param(
    val key: String?,
    val value: String?
)

// ============== Player Response ==============

@Serializable
data class PlayerResponse(
    val videoDetails: VideoDetails?,
    val streamingData: StreamingData?,
    val playabilityStatus: PlayabilityStatus?,
    val responseContext: ResponseContext?
)

@Serializable
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

@Serializable
data class StreamingData(
    val expiresInSeconds: String?,
    val formats: List<Format>?,
    val adaptiveFormats: List<Format>?,
    val hlsManifestUrl: String?,
    val dashManifestUrl: String?
)

@Serializable
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
    val cipher: String?
) {
    fun isAudioOnly(): Boolean = height == null && audioQuality != null
    fun isVideoOnly(): Boolean = audioQuality == null && height != null
    fun hasAudioAndVideo(): Boolean = audioQuality != null && height != null
}

@Serializable
data class PlayabilityStatus(
    val status: String?,
    val playableInEmbed: Boolean?,
    val reason: String?,
    val errorScreen: ErrorScreen?,
    val liveStreamability: LiveStreamability?
) {
    fun isPlayable(): Boolean = status == "OK"
}

@Serializable
data class ErrorScreen(
    val playerErrorMessageRenderer: PlayerErrorMessageRenderer?
)

@Serializable
data class PlayerErrorMessageRenderer(
    val reason: SimpleText?,
    val subreason: SimpleText?
)

@Serializable
data class LiveStreamability(
    val liveStreamabilityRenderer: LiveStreamabilityRenderer?
)

@Serializable
data class LiveStreamabilityRenderer(
    val videoId: String?,
    val broadcastId: String?
)

// ============== Browse Response ==============

@Serializable
data class BrowseResponse(
    val contents: BrowseContents?,
    val header: BrowseHeader?,
    val continuationContents: ContinuationContents?,
    val responseContext: ResponseContext?
)

@Serializable
data class BrowseContents(
    val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer?,
    val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?,
    val sectionListRenderer: SectionListRenderer?
)

@Serializable
data class SingleColumnBrowseResultsRenderer(
    val tabs: List<Tab>?
)

@Serializable
data class TwoColumnBrowseResultsRenderer(
    val tabs: List<Tab>?,
    val secondaryContents: SecondaryContents?
)

@Serializable
data class SecondaryContents(
    val sectionListRenderer: SectionListRenderer?
)

@Serializable
data class BrowseHeader(
    val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
    val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?
)

@Serializable
data class MusicImmersiveHeaderRenderer(
    val title: TextRuns?,
    val description: TextRuns?,
    val thumbnail: MusicThumbnailRenderer?,
    val playButton: PlayButton?,
    val startRadioButton: StartRadioButton?,
    val menu: Menu?
)

@Serializable
data class MusicDetailHeaderRenderer(
    val title: TextRuns?,
    val subtitle: TextRuns?,
    val thumbnail: MusicThumbnailRenderer?,
    val menu: Menu?,
    val secondSubtitle: TextRuns?
)

@Serializable
data class PlayButton(
    val buttonRenderer: ButtonRenderer?
)

@Serializable
data class StartRadioButton(
    val buttonRenderer: ButtonRenderer?
)

@Serializable
data class ButtonRenderer(
    val navigationEndpoint: NavigationEndpoint?,
    val text: TextRuns?
)

@Serializable
data class Menu(
    val menuRenderer: MenuRenderer?
)

@Serializable
data class MenuRenderer(
    val items: List<MenuItem>?
)

@Serializable
data class MenuItem(
    val menuNavigationItemRenderer: MenuNavigationItemRenderer?,
    val menuServiceItemRenderer: MenuServiceItemRenderer?
)

@Serializable
data class MenuNavigationItemRenderer(
    val text: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

@Serializable
data class MenuServiceItemRenderer(
    val text: TextRuns?,
    val serviceEndpoint: ServiceEndpoint?
)

@Serializable
data class ServiceEndpoint(
    val playlistEditEndpoint: PlaylistEditEndpoint?
)

@Serializable
data class PlaylistEditEndpoint(
    val playlistId: String?,
    val actions: List<PlaylistEditAction>?
)

@Serializable
data class PlaylistEditAction(
    val setVideoId: String?,
    val removedVideoId: String?,
    val addedVideoId: String?
)

// ============== Next Response (Related/Suggestions) ==============

@Serializable
data class NextResponse(
    val contents: NextContents?,
    val currentVideoEndpoint: NavigationEndpoint?,
    val responseContext: ResponseContext?
)

@Serializable
data class NextContents(
    val twoColumnWatchNextResults: TwoColumnWatchNextResults?,
    val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?
)

@Serializable
data class TwoColumnWatchNextResults(
    val results: WatchNextResults?,
    val secondaryResults: SecondaryResults?
)

@Serializable
data class WatchNextResults(
    val results: ResultsContents?
)

@Serializable
data class ResultsContents(
    val contents: List<WatchNextContent>?
)

@Serializable
data class WatchNextContent(
    val videoPrimaryInfoRenderer: VideoPrimaryInfoRenderer?,
    val videoSecondaryInfoRenderer: VideoSecondaryInfoRenderer?
)

@Serializable
data class VideoPrimaryInfoRenderer(
    val title: TextRuns?,
    val viewCount: ViewCount?,
    val dateText: SimpleText?
)

@Serializable
data class ViewCount(
    val videoViewCountRenderer: VideoViewCountRenderer?
)

@Serializable
data class VideoViewCountRenderer(
    val viewCount: SimpleText?,
    val shortViewCount: SimpleText?
)

@Serializable
data class VideoSecondaryInfoRenderer(
    val owner: Owner?,
    val description: TextRuns?
)

@Serializable
data class Owner(
    val videoOwnerRenderer: VideoOwnerRenderer?
)

@Serializable
data class VideoOwnerRenderer(
    val title: TextRuns?,
    val thumbnail: Thumbnails?,
    val subscriberCountText: SimpleText?
)

@Serializable
data class SecondaryResults(
    val secondaryResults: SecondaryResultsContent?
)

@Serializable
data class SecondaryResultsContent(
    val results: List<SecondaryResultItem>?
)

@Serializable
data class SecondaryResultItem(
    val compactVideoRenderer: CompactVideoRenderer?,
    val compactPlaylistRenderer: CompactPlaylistRenderer?
)

@Serializable
data class CompactPlaylistRenderer(
    val playlistId: String?,
    val title: SimpleText?,
    val thumbnail: Thumbnails?,
    val videoCountText: TextRuns?
)

@Serializable
data class SingleColumnMusicWatchNextResultsRenderer(
    val tabbedRenderer: TabbedRenderer?
)

@Serializable
data class TabbedRenderer(
    val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?
)

@Serializable
data class WatchNextTabbedResultsRenderer(
    val tabs: List<Tab>?
)

// ============== Music Search Suggestions ==============

@Serializable
data class MusicSearchSuggestionsResponse(
    val contents: List<MusicSearchSuggestionContent>?,
    val responseContext: ResponseContext?
)

@Serializable
data class MusicSearchSuggestionContent(
    val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer?
)

@Serializable
data class SearchSuggestionsSectionRenderer(
    val contents: List<SearchSuggestionItem>?
)

@Serializable
data class SearchSuggestionItem(
    val searchSuggestionRenderer: SearchSuggestionRenderer?
)

@Serializable
data class SearchSuggestionRenderer(
    val suggestion: TextRuns?,
    val navigationEndpoint: NavigationEndpoint?
)

// ============== Music Queue ==============

@Serializable
data class MusicQueueResponse(
    val queueDatas: List<QueueData>?,
    val responseContext: ResponseContext?
)

@Serializable
data class QueueData(
    val content: QueueContent?
)

@Serializable
data class QueueContent(
    val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?
)

@Serializable
data class PlaylistPanelVideoRenderer(
    val title: TextRuns?,
    val longBylineText: TextRuns?,
    val thumbnail: Thumbnails?,
    val lengthText: SimpleText?,
    val videoId: String?,
    val navigationEndpoint: NavigationEndpoint?
)
