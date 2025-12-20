package com.jgm90.cloudmusic.feature.home.presentation

import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.viewinterop.AndroidView
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.ExperimentalMaterial3Api
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.DownloadEvent
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.core.ui.theme.CloudMusicTheme
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.feature.playback.presentation.NowPlayingActivity
import com.jgm90.cloudmusic.feature.playback.presentation.PlaybackControlsBar
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import com.jgm90.cloudmusic.feature.playlist.presentation.PlaylistDetailActivity
import com.jgm90.cloudmusic.feature.playlist.presentation.PlaylistScreen
import com.jgm90.cloudmusic.feature.search.presentation.SearchScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
                    versionName = versionName,
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
        if (!isChangingConfigurations) {
            player_service?.stopSelf()
        }
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
    Playlists,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    versionName: String,
    showChangelog: Boolean,
    showPlayback: Boolean,
    onChangelogDismiss: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenNowPlayingWithList: (Int, List<com.jgm90.cloudmusic.core.model.SongModel>) -> Unit,
    onOpenPlaylist: (PlaylistModel) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var destination by remember { mutableStateOf(HomeDestination.Search) }
    var showAbout by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(showChangelog) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight(),
            ) {
                DrawerHeader(versionName = versionName)
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.search)) },
                    selected = destination == HomeDestination.Search,
                    onClick = {
                        destination = HomeDestination.Search
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_black_24dp),
                            contentDescription = null,
                        )
                    },
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.playlists)) },
                    selected = destination == HomeDestination.Playlists,
                    onClick = {
                        destination = HomeDestination.Playlists
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_library_music_black_24dp),
                            contentDescription = null,
                        )
                    },
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.about)) },
                    selected = false,
                    onClick = {
                        showAbout = true
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_info_black_24dp),
                            contentDescription = null,
                        )
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (destination) {
                                HomeDestination.Search -> stringResource(R.string.search)
                                HomeDestination.Playlists -> stringResource(R.string.playlists)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert_black_24dp),
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
            bottomBar = {
                if (showPlayback) {
                    PlaybackControlsBar(onOpenNowPlaying = onOpenNowPlaying)
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (destination) {
                    HomeDestination.Search -> SearchScreen(onOpenNowPlaying = onOpenNowPlayingWithList)
                    HomeDestination.Playlists -> PlaylistScreen(onOpenPlaylist = onOpenPlaylist)
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(text = stringResource(id = R.string.about)) },
            text = { HtmlText(html = stringResource(id = R.string.about_body)) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(text = "OK") }
            },
        )
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
private fun DrawerHeader(versionName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (versionName.isNotEmpty()) {
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
