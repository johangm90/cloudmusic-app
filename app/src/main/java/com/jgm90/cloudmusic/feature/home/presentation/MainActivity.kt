package com.jgm90.cloudmusic.feature.home.presentation

import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.DownloadEvent
import com.jgm90.cloudmusic.core.ui.theme.AppBackground
import com.jgm90.cloudmusic.core.ui.theme.CloudMusicTheme
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.feature.playback.presentation.NowPlayingActivity
import com.jgm90.cloudmusic.feature.playback.presentation.PlaybackControlsBar
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import com.jgm90.cloudmusic.feature.playlist.presentation.LibraryScreen
import com.jgm90.cloudmusic.feature.playlist.presentation.LikedSongsScreen
import com.jgm90.cloudmusic.feature.playlist.presentation.PlaylistDetailActivity
import com.jgm90.cloudmusic.feature.playlist.presentation.RecentlyPlayedScreen
import com.jgm90.cloudmusic.feature.search.presentation.SearchScreen
import com.jgm90.cloudmusic.feature.settings.presentation.screen.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private var packageInfo: PackageInfo? = null
    private var eventJobs: List<Job> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        packageInfo = runCatching {
            packageManager.getPackageInfo(packageName, 0)
        }.getOrNull()
        val versionName = packageInfo?.versionName.orEmpty()
        val shouldShowChangelog = SharedUtils.get_version(this) != versionName

        setContent {
            CloudMusicTheme {
                val showPlayback by playbackControlsVisible
                MainContent(
                    showChangelog = shouldShowChangelog,
                    showPlayback = showPlayback,
                    onChangelogDismiss = {
                        SharedUtils.set_version(this@MainActivity, versionName)
                    },
                    onOpenNowPlaying = { openNowPlaying() },
                    onOpenNowPlayingWithList = { index, list -> openNowPlaying(index, list) },
                    onOpenPlaylist = { playlist -> openPlaylistDetail(playlist) },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        eventJobs = listOf(
            AppEventBus.observe<DownloadEvent>(lifecycleScope) { event ->
                download(event)
                AppEventBus.clearSticky(DownloadEvent::class)
            },
        )
    }

    override fun onStop() {
        super.onStop()
        eventJobs.forEach { it.cancel() }
        eventJobs = emptyList()
    }

    private fun openNowPlaying(index: Int, list: List<com.jgm90.cloudmusic.core.model.SongModel>) {
        val intent = Intent(this, NowPlayingActivity::class.java)
        intent.putExtra("SONG_INDEX", index)
        NowPlayingActivity.audioList = list.toMutableList()
        startActivity(intent)
    }

    private fun openNowPlaying() {
        val intent = Intent(this, NowPlayingActivity::class.java)
        startActivity(intent)
    }

    private fun openPlaylistDetail(playlist: PlaylistModel) {
        val intent = Intent(this, PlaylistDetailActivity::class.java)
        intent.putExtra("PLAYLIST_ID", playlist.playlist_id)
        intent.putExtra("PLAYLIST_NAME", playlist.name)
        intent.putExtra("PLAYLIST_COUNT", playlist.song_count)
        intent.putExtra("PLAYLIST_OFFLINE", playlist.offline)
        startActivity(intent)
    }

    private fun download(event: DownloadEvent) {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val uri = event.url.toUri()
        val request = DownloadManager.Request(uri)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, event.filename)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        downloadManager.enqueue(request)
    }
}

private enum class HomeDestination {
    Search,
    Library,
    RecentlyPlayed,
    LikedSongs,
    Settings,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    showChangelog: Boolean,
    showPlayback: Boolean,
    onChangelogDismiss: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenNowPlayingWithList: (Int, List<com.jgm90.cloudmusic.core.model.SongModel>) -> Unit,
    onOpenPlaylist: (PlaylistModel) -> Unit,
) {
    var destination by remember { mutableStateOf(HomeDestination.Search) }
    var showChangelogDialog by remember { mutableStateOf(showChangelog) }
    val canNavigateBack =
        destination == HomeDestination.RecentlyPlayed ||
        destination == HomeDestination.LikedSongs ||
        destination == HomeDestination.Settings
    var searchExpanded by remember { mutableStateOf(false) }
    val showTopBar = !canNavigateBack && !(destination == HomeDestination.Search && searchExpanded)

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (showTopBar) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (destination) {
                                    HomeDestination.Search -> stringResource(R.string.search)
                                    HomeDestination.Library -> stringResource(R.string.library)
                                    HomeDestination.RecentlyPlayed -> stringResource(R.string.recently_played)
                                    HomeDestination.LikedSongs -> stringResource(R.string.liked_songs)
                                    HomeDestination.Settings -> stringResource(R.string.settings)
                                }
                            )
                        },
                        actions = {
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White,
                        ),
                    )
                }
            },
            bottomBar = {
                Column {
                    if (showPlayback) {
                        PlaybackControlsBar(onOpenNowPlaying = onOpenNowPlaying)
                    }
                    NavigationBar(
                        containerColor = Color(0xCC0F171E),
                        contentColor = Color.White,
                    ) {
                        NavigationBarItem(
                            selected = destination == HomeDestination.Search,
                            onClick = { destination = HomeDestination.Search },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search_black_24dp),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(text = stringResource(R.string.search)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                indicatorColor = Color.Transparent,
                            )
                        )
                        NavigationBarItem(
                            selected = destination == HomeDestination.Library,
                            onClick = { destination = HomeDestination.Library },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_library_music_black_24dp),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(text = stringResource(R.string.library)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                indicatorColor = Color.Transparent,
                            )
                        )
                        NavigationBarItem(
                            selected = destination == HomeDestination.Settings,
                            onClick = { destination = HomeDestination.Settings },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings_black_24dp),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(text = stringResource(R.string.settings)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                indicatorColor = Color.Transparent,
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (destination) {
                    HomeDestination.Search -> SearchScreen(
                        onOpenNowPlaying = onOpenNowPlayingWithList,
                        onSearchActiveChange = { searchExpanded = it },
                    )
                    HomeDestination.Library -> LibraryScreen(
                        onOpenRecent = { destination = HomeDestination.RecentlyPlayed },
                        onOpenLiked = { destination = HomeDestination.LikedSongs },
                        onOpenPlaylist = onOpenPlaylist,
                    )

                    HomeDestination.RecentlyPlayed -> RecentlyPlayedScreen(
                        onBack = { destination = HomeDestination.Library },
                        onOpenNowPlaying = onOpenNowPlayingWithList,
                    )

                    HomeDestination.LikedSongs -> LikedSongsScreen(
                        onBack = { destination = HomeDestination.Library },
                        onOpenNowPlaying = onOpenNowPlayingWithList,
                    )

                    HomeDestination.Settings -> SettingsScreen(
                        onBack = { destination = HomeDestination.Search },
                    )
                }
            }
        }
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangelogDialog = false
                onChangelogDismiss()
            },
            title = { Text(text = "Changelog") },
            text = {
                HtmlText(html = stringResource(id = R.string.changelog))
            },
            confirmButton = {
                TextButton(onClick = {
                    showChangelogDialog = false
                    onChangelogDismiss()
                }) { Text(text = "OK") }
            },
        )
    }
}

@Composable
private fun HtmlText(html: String) {
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
    )
}
