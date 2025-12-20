package com.jgm90.cloudmusic.fragments

import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.R2
import com.jgm90.cloudmusic.adapters.PlaylistsAdapter
import com.jgm90.cloudmusic.data.PlaylistData
import com.jgm90.cloudmusic.dialogs.PlaylistDialog
import com.jgm90.cloudmusic.interfaces.DialogCaller
import com.jgm90.cloudmusic.models.PlaylistModel
import com.jgm90.cloudmusic.utils.Divider
import com.jgm90.cloudmusic.utils.SharedUtils
import com.jgm90.cloudmusic.widgets.VulgryMessageView

class PlaylistFragment : Fragment(), SearchView.OnQueryTextListener, DialogCaller {
    @JvmField
    @BindView(R2.id.message_view)
    var message_view: VulgryMessageView? = null

    @JvmField
    @BindView(R2.id.rv_playlists)
    var mRecyclerView: RecyclerView? = null

    var search_query: String? = null
    private var listState: Parcelable? = null
    private var mAdapter: PlaylistsAdapter? = null
    private var mModel: MutableList<PlaylistModel> = mutableListOf()
    private var hostActivity: AppCompatActivity? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var unbinder: Unbinder? = null
    private var searchView: SearchView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_playlist, container, false)
        hostActivity = activity as? AppCompatActivity
        unbinder = ButterKnife.bind(this, rootView)
        mLayoutManager = LinearLayoutManager(hostActivity)
        mRecyclerView?.layoutManager = mLayoutManager
        mRecyclerView?.setHasFixedSize(true)
        mRecyclerView?.addItemDecoration(Divider(requireContext()))
        mRecyclerView?.itemAnimator?.addDuration = SharedUtils.rv_anim_duration.toLong()
        mRecyclerView?.adapter = null
        mModel = ArrayList()
        setHasOptionsMenu(true)
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable(LIST_STATE_KEY)
            mModel = savedInstanceState.getParcelableArrayList(LIST_ARRAY) ?: ArrayList()
            hostActivity?.let { activity ->
                mAdapter = PlaylistsAdapter(mModel, activity, this)
                mRecyclerView?.adapter = mAdapter
                mAdapter?.notifyDataSetChanged()
            }
        }
        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        listState = mLayoutManager?.onSaveInstanceState()
        outState.putParcelable(LIST_STATE_KEY, listState)
        outState.putParcelableArrayList(LIST_ARRAY, ArrayList(mModel))
    }

    override fun onResume() {
        super.onResume()
        reload()
        listState?.let { mLayoutManager?.onRestoreInstanceState(it) }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            reload()
        }
    }

    @OnClick(R2.id.message_view)
    fun reload() {
        message_view?.visibility = View.GONE
        mModel.clear()
        mRecyclerView?.adapter = null
        getPlaylists()
    }

    private fun getPlaylists() {
        try {
            val dao = PlaylistData(requireContext())
            mModel = dao.getAll().toMutableList()
            if (mModel.isNotEmpty()) {
                hostActivity?.let { activity ->
                    mAdapter = PlaylistsAdapter(mModel, activity, this)
                    mRecyclerView?.adapter = mAdapter
                    mAdapter?.notifyItemChanged(0)
                }
            } else {
                SharedUtils.showMessage(
                    message_view,
                    R.drawable.ic_library_music_black_24dp,
                    R.string.no_playlists,
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_playlists, menu)
        val item = menu.findItem(R.id.action_search)
        searchView = MenuItemCompat.getActionView(item) as SearchView
        searchView?.queryHint = "Buscar"
        searchView?.setOnQueryTextListener(this)
        if (!TextUtils.isEmpty(search_query)) {
            searchView?.setQuery(search_query, false)
            searchView?.isIconified = false
            searchView?.clearFocus()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_add) {
            PlaylistDialog(requireContext(), this).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextChange(newText: String): Boolean {
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onPositiveCall() {
        reload()
    }

    companion object {
        const val LIST_STATE_KEY = "recycler_list_state"
        const val LIST_ARRAY = "recycler_list_model"
    }
}
