package com.jgm90.cloudmusic.feature.home.presentation

import android.app.DownloadManager
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.navigation.NavigationView
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.DownloadEvent
import com.jgm90.cloudmusic.core.app.BaseActivity
import com.jgm90.cloudmusic.databinding.ActivityMainBinding
import com.jgm90.cloudmusic.feature.search.presentation.SearchFragment
import com.jgm90.cloudmusic.feature.playlist.presentation.PlaylistFragment
import com.jgm90.cloudmusic.core.util.SharedUtils
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import androidx.core.net.toUri

@AndroidEntryPoint
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val SF_TAG = "sf_tag"
    private val PL_TAG = "pl_tag"
    private var searchFragment: SearchFragment? = null
    private var playlistFragment: PlaylistFragment? = null
    private var packageInfo: PackageInfo? = null
    private var eventJobs: List<Job> = emptyList()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = binding.appBarMain.toolbar
        setSupportActionBar(toolbar)
        val drawer = binding.drawerLayout
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        val navigationView = binding.navView
        navigationView.setNavigationItemSelectedListener(this)
        try {
            val headerView = navigationView.inflateHeaderView(R.layout.nav_header_main)
            val lbl_version = headerView.findViewById<TextView>(R.id.lbl_version)
            packageInfo = packageManager.getPackageInfo(getPackageName(), 0)
            lbl_version.text = "v" + packageInfo!!.versionName
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (savedInstanceState == null) {
            searchFragment = SearchFragment()
            playlistFragment = PlaylistFragment()
        } else {
            searchFragment =
                supportFragmentManager.findFragmentByTag(SF_TAG) as SearchFragment?
            if (searchFragment == null) {
                searchFragment = SearchFragment()
            }
            playlistFragment =
                supportFragmentManager.findFragmentByTag(PL_TAG) as PlaylistFragment?
            if (playlistFragment == null) {
                playlistFragment = PlaylistFragment()
            }
        }
        showChangelog()
    }

    fun showChangelog() {
        if (SharedUtils.get_version(this) != packageInfo!!.versionName) {
            MaterialDialog.Builder(this)
                .title("Changelog")
                .content(Html.fromHtml(getString(R.string.changelog)))
                .cancelable(false)
                .positiveText("Cerrar")
                .onPositive { _, _ ->
                    SharedUtils.set_version(
                        this@MainActivity,
                        packageInfo!!.versionName
                    )
                }
                .contentLineSpacing(1.6f)
                .show()
        }
    }

    @Deprecated("")
    override fun onBackPressed() {
        super.onBackPressed()
        val drawer = binding.drawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            moveTaskToBack(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isChangingConfigurations) {
            Log.i("App", "Main Activity is changing configurations")
        } else {
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val unicode = 0x2764
        val emoji = getEmojiByUnicode(unicode)
        val id = item.getItemId()
        if (id == R.id.nav_buscar) {
            showSearchFragment()
        } else if (id == R.id.nav_playlists) {
            showPlaylistFragment()
        } else if (id == R.id.nav_about) {
            MaterialDialog.Builder(this)
                .title(R.string.about)
                .positiveText("Cerrar")
                .content(Html.fromHtml("<b>Cloud Music</b>, made with " + emoji + " by <br/><b>Johan Guerreros</b>."))
                .contentLineSpacing(1.6f)
                .show()
        }
        val drawer = binding.drawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }

    private fun showSearchFragment() {
        val ft = supportFragmentManager.beginTransaction()
        if (searchFragment!!.isAdded) {
            ft.show(searchFragment!!)
        } else {
            ft.add(R.id.container_body, searchFragment!!, SF_TAG)
        }
        if (playlistFragment!!.isAdded) {
            ft.hide(playlistFragment!!)
        }
        ft.commit()
    }

    private fun showPlaylistFragment() {
        val ft = supportFragmentManager.beginTransaction()
        if (playlistFragment!!.isAdded) {
            ft.show(playlistFragment!!)
        } else {
            ft.add(R.id.container_body, playlistFragment!!, PL_TAG)
        }
        if (searchFragment!!.isAdded) {
            ft.hide(searchFragment!!)
        }
        ft.commit()
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
