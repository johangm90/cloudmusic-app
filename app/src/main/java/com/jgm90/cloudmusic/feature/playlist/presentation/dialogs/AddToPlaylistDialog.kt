package com.jgm90.cloudmusic.feature.playlist.presentation.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.jgm90.cloudmusic.databinding.DialogAddToPlaylistBinding
import com.jgm90.cloudmusic.feature.playlist.presentation.adapter.PlaylistsAdapter
import com.jgm90.cloudmusic.feature.playlist.data.PlaylistData
import com.jgm90.cloudmusic.feature.playlist.data.SongData
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.DialogCaller
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.ListCaller
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.ui.decoration.Divider
import com.jgm90.cloudmusic.core.util.SharedUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddToPlaylistDialog(private val context: Context) : ListCaller, DialogCaller {
    private var dialog: MaterialDialog? = null
    private var binding: DialogAddToPlaylistBinding? = null
    private var adapter: PlaylistsAdapter? = null
    private var searchObj: SongModel? = null
    private var songObj: SongModel? = null
    private val dao: SongData = SongData(context)
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
            PlaylistDialog(context, this).show()
        }
        dialogBinding.rvPlaylists.adapter = null
        dialogBinding.rvPlaylists.layoutManager = LinearLayoutManager(context)
        dialogBinding.rvPlaylists.setHasFixedSize(true)
        dialogBinding.rvPlaylists.addItemDecoration(Divider(context))
        dialogBinding.rvPlaylists.itemAnimator?.addDuration = SharedUtils.rv_anim_duration.toLong()
        reload()
    }

    private fun reload() {
        val dao = PlaylistData(context)
        uiScope.launch {
            val playlists = withContext(Dispatchers.IO) { dao.getAll() }
            if (playlists.isNotEmpty()) {
                adapter = PlaylistsAdapter(playlists.toMutableList(), context, this@AddToPlaylistDialog as ListCaller)
                binding?.rvPlaylists?.adapter = adapter
                adapter?.notifyItemChanged(0)
            }
        }
    }

    override fun onListItemClick(itemId: Int) {
        val searchItem = searchObj ?: return
        uiScope.launch {
            val nextPosition = withContext(Dispatchers.IO) { dao.getNextPosition() }
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
                nextPosition,
                SharedUtils.dateTime,
                itemId,
            )
            withContext(Dispatchers.IO) { dao.insert(songObj!!) }
            Toast.makeText(context, "Item added to playlist", Toast.LENGTH_SHORT).show()
            dialog?.dismiss()
        }
    }

    override fun onPositiveCall() {
        reload()
    }
}
