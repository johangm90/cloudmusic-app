package com.jgm90.cloudmusic.activities

import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import butterknife.ButterKnife
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback
import com.google.android.material.navigation.NavigationView
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.events.DownloadEvent
import com.jgm90.cloudmusic.fragments.PlaylistFragment
import com.jgm90.cloudmusic.fragments.SearchFragment
import com.jgm90.cloudmusic.models.UpdateModel
import com.jgm90.cloudmusic.utils.RestClient
import com.jgm90.cloudmusic.utils.SharedUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Exception

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val SF_TAG = "sf_tag"
    private val PL_TAG = "pl_tag"
    private var searchFragment: SearchFragment? = null
    private var playlistFragment: PlaylistFragment? = null
    private var packageInfo: PackageInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.setDrawerListener(toggle)
        toggle.syncState()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        try {
            val headerView = navigationView.inflateHeaderView(R.layout.nav_header_main)
            val lbl_version = headerView.findViewById<TextView>(R.id.lbl_version)
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0)
            lbl_version.setText("v" + packageInfo!!.versionName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ButterKnife.bind(this)
        if (savedInstanceState == null) {
            searchFragment = SearchFragment()
            playlistFragment = PlaylistFragment()
        } else {
            searchFragment =
                getSupportFragmentManager().findFragmentByTag(SF_TAG) as SearchFragment?
            if (searchFragment == null) {
                searchFragment = SearchFragment()
            }
            playlistFragment =
                getSupportFragmentManager().findFragmentByTag(PL_TAG) as PlaylistFragment?
            if (playlistFragment == null) {
                playlistFragment = PlaylistFragment()
            }
        }
        EventBus.getDefault().register(this)
        checkForUpdate()
        showChangelog()
    }

    fun checkForUpdate() {
        try {
            val api = RestClient.build(SharedUtils.server)
            val call = api.checkUpdate()
            call.enqueue(object : Callback<UpdateModel?> {
                override fun onResponse(
                    call: Call<UpdateModel?>?,
                    response: Response<UpdateModel?>?
                ) {
                    if (response!!.isSuccessful) {
                        if (response.body() != null) {
                            val version = response.body()!!.version
                            val url = response.body()!!.download_url
                            val date = response.body()!!.published_at
                            val local_version = packageInfo!!.versionName!!.toDouble()
                            val online_version = version.toDouble()
                            if (online_version > local_version) {
                                MaterialDialog.Builder(this@MainActivity)
                                    .title("Update available")
                                    .content("Version: " + version + "\nPublished at: " + date)
                                    .cancelable(false)
                                    .positiveText("Descargar")
                                    .onPositive(object : SingleButtonCallback {
                                        override fun onClick(
                                            dialog: MaterialDialog,
                                            which: DialogAction
                                        ) {
                                            val uri = Uri.parse(url)
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            startActivity(intent)
                                        }
                                    })
                                    .negativeText("Cancelar")
                                    .show()
                            }
                        }
                    } else {
                        Log.e("App", "Server error: " + response.code())
                    }
                }

                override fun onFailure(call: Call<UpdateModel?>?, t: Throwable?) {
                    Log.e("App", "Server error")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showChangelog() {
        if (SharedUtils.get_version(this) != packageInfo!!.versionName) {
            MaterialDialog.Builder(this)
                .title("Changelog")
                .content(Html.fromHtml(getString(R.string.changelog)))
                .cancelable(false)
                .positiveText("Cerrar")
                .onPositive(object : SingleButtonCallback {
                    override fun onClick(dialog: MaterialDialog, which: DialogAction) {
                        SharedUtils.set_version(this@MainActivity, packageInfo!!.versionName)
                    }
                })
                .contentLineSpacing(1.6f)
                .show()
        }
    }

    @Deprecated("")
    override fun onBackPressed() {
        super.onBackPressed()
        val drawer = findViewById<View?>(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            moveTaskToBack(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        if (isChangingConfigurations) {
            Log.i("App", "Main Activity is changing configurations")
        } else {
            player_service.stopSelf()
        }
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
        val drawer = findViewById<View?>(R.id.drawer_layout) as DrawerLayout
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

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun download(event: DownloadEvent) {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(event.url)
        val request = DownloadManager.Request(uri)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, event.filename)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        downloadManager.enqueue(request)
    }
}
