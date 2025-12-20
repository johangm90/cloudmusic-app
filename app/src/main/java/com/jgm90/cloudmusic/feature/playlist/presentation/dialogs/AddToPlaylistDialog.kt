package com.jgm90.cloudmusic.feature.playlist.presentation.dialogs

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.feature.playlist.presentation.adapter.PlaylistsAdapter
import com.jgm90.cloudmusic.feature.playlist.data.PlaylistData
import com.jgm90.cloudmusic.feature.playlist.data.SongData
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.DialogCaller
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.ListCaller
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.ui.decoration.Divider
import com.jgm90.cloudmusic.core.util.SharedUtils

class AddToPlaylistDialog(private val context: Context) : ListCaller, DialogCaller {
    private var dialog: MaterialDialog? = null
    private var dialogView: View? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: PlaylistsAdapter? = null
    private var searchObj: SongModel? = null
    private var songObj: SongModel? = null
    private val dao: SongData = SongData(context)
    private var btnAdd: AppCompatButton? = null

    fun show(obj: SongModel) {
        searchObj = obj
        val dialogBuilder = MaterialDialog.Builder(context)
            .title("Add to playlist")
            .customView(R.layout.dialog_add_to_playlist, false)
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
        dialogView = dialog?.customView
        bind()
    }

    private fun bind() {
        val view = dialogView ?: return
        recyclerView = view.findViewById(R.id.rv_playlists)
        btnAdd = view.findViewById(R.id.btn_add)
        btnAdd?.setOnClickListener {
            PlaylistDialog(context, this).show()
        }
        recyclerView?.adapter = null
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.addItemDecoration(Divider(context))
        recyclerView?.itemAnimator?.addDuration = SharedUtils.rv_anim_duration.toLong()
        reload()
    }

    private fun reload() {
        val dao = PlaylistData(context)
        val playlists: List<PlaylistModel> = dao.getAll()
        if (playlists.isNotEmpty()) {
            adapter = PlaylistsAdapter(playlists.toMutableList(), context, this as ListCaller)
            recyclerView?.adapter = adapter
            adapter?.notifyItemChanged(0)
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
            dao.getNextPosition(),
            SharedUtils.dateTime,
            itemId,
        )
        dao.insert(songObj!!)
        Toast.makeText(context, "Item added to playlist", Toast.LENGTH_SHORT).show()
        dialog?.dismiss()
    }

    override fun onPositiveCall() {
        reload()
    }
}
