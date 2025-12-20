package com.jgm90.cloudmusic.feature.playlist.presentation.dialogs

import android.content.Context
import android.view.LayoutInflater
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.jgm90.cloudmusic.databinding.DialogNewPlaylistBinding
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.DialogCaller
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel

class PlaylistDialog(
    private val context: Context,
    private val listener: DialogCaller?,
    private val onSave: (PlaylistModel) -> Unit,
) {
    private var dialog: MaterialDialog? = null
    private var binding: DialogNewPlaylistBinding? = null
    private var obj: PlaylistModel? = null
    private var cargado = false

    fun show() {
        binding = DialogNewPlaylistBinding.inflate(LayoutInflater.from(context))
        val dialogBuilder = MaterialDialog.Builder(context)
            .title("Playlist")
            .customView(binding!!.root, false)
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
        bind()
    }

    private fun bind() {
        if (cargado) {
            binding?.txtPlaylistName?.setText(obj?.name)
        }
    }

    private fun validate(): Boolean {
        return !binding?.txtPlaylistName?.text.isNullOrEmpty()
    }

    private fun guardar() {
        val item = PlaylistModel(
            0,
            binding?.txtPlaylistName?.text?.toString().orEmpty(),
            0,
        )
        if (cargado) {
            item.playlist_id = obj?.playlist_id ?: 0
        }
        onSave(item)
    }

    fun cargar(obj: PlaylistModel) {
        this.obj = obj
        cargado = true
        show()
    }
}
