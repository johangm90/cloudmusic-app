package com.jgm90.cloudmusic.feature.playlist.presentation

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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.core.ui.decoration.Divider
import com.jgm90.cloudmusic.core.util.SharedUtils
import com.jgm90.cloudmusic.databinding.FragmentPlaylistBinding
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import com.jgm90.cloudmusic.feature.playlist.presentation.adapter.PlaylistsAdapter
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.DialogCaller
import com.jgm90.cloudmusic.feature.playlist.presentation.dialogs.PlaylistDialog
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistFragment : Fragment(), SearchView.OnQueryTextListener, DialogCaller {
    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    var search_query: String? = null
    private var listState: Parcelable? = null
    private var mAdapter: PlaylistsAdapter? = null
    private var mModel: MutableList<PlaylistModel> = mutableListOf()
    private var hostActivity: AppCompatActivity? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var searchView: SearchView? = null
    private val viewModel by viewModels<PlaylistViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        val rootView = binding.root
        hostActivity = activity as? AppCompatActivity
        mLayoutManager = LinearLayoutManager(hostActivity)
        binding.rvPlaylists.layoutManager = mLayoutManager
        binding.rvPlaylists.setHasFixedSize(true)
        binding.rvPlaylists.addItemDecoration(Divider(requireContext()))
        binding.rvPlaylists.itemAnimator?.addDuration = SharedUtils.rv_anim_duration.toLong()
        binding.rvPlaylists.adapter = null
        binding.messageView.setOnClickListener { reload() }
        mModel = ArrayList()
        setHasOptionsMenu(true)
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable(LIST_STATE_KEY)
            mModel = savedInstanceState.getParcelableArrayList(LIST_ARRAY) ?: ArrayList()
            mAdapter = PlaylistsAdapter(mModel, requireContext())
            binding.rvPlaylists.adapter = mAdapter
            mAdapter?.notifyDataSetChanged()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun reload() {
        binding.messageView.visibility = View.GONE
        mModel.clear()
        binding.rvPlaylists.adapter = null
        getPlaylists()
    }

    private fun getPlaylists() {
        viewModel.loadPlaylists { playlists ->
            mModel = playlists.toMutableList()
            if (mModel.isNotEmpty()) {
                hostActivity?.let { activity ->
                    mAdapter = PlaylistsAdapter(
                        mModel,
                        activity,
                        this@PlaylistFragment,
                        onDeletePlaylist = { playlist ->
                            viewModel.deletePlaylist(playlist) { reload() }
                        },
                        onEditPlaylist = { playlist ->
                            PlaylistDialog(
                                requireContext(),
                                this@PlaylistFragment,
                                onSave = { model -> viewModel.savePlaylist(model) { reload() } },
                            ).cargar(playlist)
                        },
                    )
                    binding.rvPlaylists.adapter = mAdapter
                    mAdapter?.notifyItemChanged(0)
                }
            } else {
                SharedUtils.showMessage(
                    binding.messageView,
                    R.drawable.ic_library_music_black_24dp,
                    R.string.no_playlists,
                )
            }
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
            PlaylistDialog(
                requireContext(),
                this,
                onSave = { model -> viewModel.savePlaylist(model) { reload() } },
            ).show()
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
