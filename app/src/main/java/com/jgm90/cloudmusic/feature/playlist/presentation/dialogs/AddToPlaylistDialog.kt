package com.jgm90.cloudmusic.feature.playlist.presentation.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.ui.decoration.Divider
import com.jgm90.cloudmusic.databinding.DialogAddToPlaylistBinding
import com.jgm90.cloudmusic.feature.playlist.presentation.adapter.PlaylistsAdapter
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.DialogCaller
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.ListCaller
import com.jgm90.cloudmusic.feature.playlist.presentation.viewmodel.PlaylistViewModel

class AddToPlaylistDialog(
    private val context: Context,
    private val viewModel: PlaylistViewModel,
) : ListCaller, DialogCaller {
    private var dialog: MaterialDialog? = null
    private var binding: DialogAddToPlaylistBinding? = null
    private var adapter: PlaylistsAdapter? = null
    private var searchObj: SongModel? = null
    private var songObj: SongModel? = null

    fun show(obj: SongModel) {
        searchObj = obj
        binding = DialogAddToPlaylistBinding.inflate(LayoutInflater.from(context))
        val dialogBuilder = MaterialDialog.Builder(context)
            .title("Add to playlist")
            .customView(binding!!.root, false)
            .cancelable(false)
            .autoDismiss(false)
            .positiveText("")
            .negativeText("CERRAR")
            .onNegative(object : MaterialDialog.SingleButtonCallback {
                override fun onClick(dialog: MaterialDialog, which: DialogAction) {
                    dialog.dismiss()
                }
            })
        dialog = dialogBuilder.build()
        dialog?.show()
        bind()
    }

    private fun bind() {
        val dialogBinding = binding ?: return
        dialogBinding.btnAdd.setOnClickListener {
            PlaylistDialog(
                context,
                this,
                onSave = { model -> viewModel.savePlaylist(model) { reload() } },
            ).show()
        }
        dialogBinding.rvPlaylists.adapter = null
        dialogBinding.rvPlaylists.layoutManager = LinearLayoutManager(context)
        dialogBinding.rvPlaylists.setHasFixedSize(true)
        dialogBinding.rvPlaylists.addItemDecoration(Divider(context))
        dialogBinding.rvPlaylists.itemAnimator?.addDuration =
            com.jgm90.cloudmusic.core.util.SharedUtils.rv_anim_duration.toLong()
        reload()
    }

    private fun reload() {
        viewModel.loadPlaylists { playlists ->
            if (playlists.isNotEmpty()) {
                adapter = PlaylistsAdapter(
                    playlists.toMutableList(),
                    context,
                    listListener = this@AddToPlaylistDialog as ListCaller
                )
                binding?.rvPlaylists?.adapter = adapter
                adapter?.notifyItemChanged(0)
            }
        }
    }

    override fun onListItemClick(itemId: Int) {
        val searchItem = searchObj ?: return
        songObj = SongModel(
            searchItem.id,
            searchItem.name,
            searchItem.artist,
            searchItem.album,
            searchItem.pic_id,
            searchItem.url_id,
            searchItem.lyric_id,
            searchItem.source,
            "",
            "",
            "",
            0,
            com.jgm90.cloudmusic.core.util.SharedUtils.dateTime,
            itemId,
        )
        viewModel.addSongToPlaylist(songObj!!, itemId) {
            Toast.makeText(context, "Item added to playlist", Toast.LENGTH_SHORT).show()
            dialog?.dismiss()
        }
    }

    override fun onPositiveCall() {
        reload()
    }
}
