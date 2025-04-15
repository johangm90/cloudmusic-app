package com.jgm90.cloudmusic.feature.search.presentation

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.getValue
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.activities.NowPlayingActivity
import com.jgm90.cloudmusic.databinding.FragmentSearchBinding
import com.jgm90.cloudmusic.dialogs.AddToPlaylistDialog
import com.jgm90.cloudmusic.events.DownloadEvent
import com.jgm90.cloudmusic.feature.search.presentation.viewmodel.SearchViewModel
import com.jgm90.cloudmusic.ui.theme.CloudMusicTheme
import io.nubit.cloudmusic.designsystem.component.EmptyState
import io.nubit.cloudmusic.designsystem.component.Loader
import io.nubit.cloudmusic.designsystem.component.SongItem
import org.greenrobot.eventbus.EventBus

class SearchFragment : Fragment(), SearchView.OnQueryTextListener {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<SearchViewModel>()
    private var searchQuery: String? = null
    private var searchView: SearchView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        val rootView = binding.root
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.setContent {
            CloudMusicTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                Loader(state.isLoading) {
                    if (state.error != null) {
                        EmptyState(
                            textRes = R.string.error_retrofit,
                            imageRes = R.drawable.ic_error_black_24dp,
                        )
                    } else if (state.searchResults.isEmpty()) {
                        EmptyState(
                            textRes = R.string.search_message,
                            imageRes = R.drawable.ic_search_black_24dp,
                        )
                    }

                    LazyColumn {
                        itemsIndexed(state.searchResults) { index, song ->
                            SongItem(
                                imageUrl = song.getCoverThumbnail(),
                                songName = song.name,
                                artistName = song.artist,
                                albumName = song.album,
                                onClick = {
                                    val intent = Intent(
                                        context,
                                        NowPlayingActivity::class.java
                                    )
                                    intent.putExtra("SONG_INDEX", index)
                                    NowPlayingActivity.audioList = state.searchResults
                                    requireContext().startActivity(intent)
                                },
                                onDownloadClick = {
                                    EventBus.getDefault().postSticky(
                                        DownloadEvent(
                                            true,
                                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                                            song.getAudioUrl(),
                                            song.name,
                                            song.getFileName()
                                        )
                                    )
                                },
                                onAddToPlaylistClick = {
                                    AddToPlaylistDialog(context).show(song)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    private fun reload() {
        searchQuery?.let {
            viewModel.search(it)
            searchView!!.clearFocus()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val item = menu.findItem(R.id.action_search)
        searchView = MenuItemCompat.getActionView(item) as SearchView
        searchView!!.queryHint = "Buscar"
        searchView!!.setOnQueryTextListener(this)
        if (!TextUtils.isEmpty(searchQuery)) {
            searchView!!.setQuery(searchQuery, false)
            searchView!!.isIconified = false
            searchView!!.clearFocus()
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        searchQuery = newText
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        reload()
        return true
    }
}

