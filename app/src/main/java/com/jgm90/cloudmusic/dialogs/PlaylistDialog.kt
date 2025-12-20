package com.jgm90.cloudmusic.dialogs

import android.content.Context
import android.view.View
import android.widget.EditText
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.data.PlaylistData
import com.jgm90.cloudmusic.interfaces.DialogCaller
import com.jgm90.cloudmusic.models.PlaylistModel

class PlaylistDialog(private val context: Context, private val listener: DialogCaller?) {
    private val dao: PlaylistData = PlaylistData(context)
    private var dialog: MaterialDialog? = null
    private var dialogView: View? = null
    private var txtPlaylistName: EditText? = null
    private var obj: PlaylistModel? = null
    private var cargado = false

    fun show() {
        val dialogBuilder = MaterialDialog.Builder(context)
            .title("Playlist")
            .customView(R.layout.dialog_new_playlist, false)
            .cancelable(false)
            .autoDismiss(false)
            .positiveText("OK")
            .negativeText("CANCEL")
            .onPositive(object : MaterialDialog.SingleButtonCallback {
                override fun onClick(dialog: MaterialDialog, which: DialogAction) {
                    if (validate()) {
                        guardar()
                        dialog.dismiss()
                        listener?.onPositiveCall()
                    }
                }
            })
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
        txtPlaylistName = dialogView?.findViewById(R.id.txt_playlist_name)
        if (cargado) {
            txtPlaylistName?.setText(obj?.name)
        }
    }

    private fun validate(): Boolean {
        return !txtPlaylistName?.text.isNullOrEmpty()
    }

    private fun guardar() {
        val item = PlaylistModel(
            0,
            txtPlaylistName?.text?.toString().orEmpty(),
            0,
        )
        if (cargado) {
            item.playlist_id = obj?.playlist_id ?: 0
            dao.update(item)
        } else {
            dao.insert(item)
        }
    }

    fun cargar(obj: PlaylistModel) {
        this.obj = obj
        cargado = true
        show()
    }
}
