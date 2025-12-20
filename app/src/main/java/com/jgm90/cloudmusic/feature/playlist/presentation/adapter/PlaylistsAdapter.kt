package com.jgm90.cloudmusic.feature.playlist.presentation.adapter

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.R2
import com.jgm90.cloudmusic.feature.playlist.presentation.PlaylistDetailActivity
import com.jgm90.cloudmusic.feature.playlist.data.PlaylistData
import com.jgm90.cloudmusic.feature.playlist.presentation.dialogs.PlaylistDialog
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.DialogCaller
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.ListCaller
import com.jgm90.cloudmusic.feature.playlist.model.PlaylistModel

class PlaylistsAdapter private constructor(
    private val model: MutableList<PlaylistModel>,
    private val context: Context,
    private val dialogListener: DialogCaller?,
    private val listListener: ListCaller?,
) : RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val dao: PlaylistData? = dialogListener?.let { PlaylistData(context) }

    constructor(model: MutableList<PlaylistModel>, context: Context, listener: DialogCaller) : this(
        model = model,
        context = context,
        dialogListener = listener,
        listListener = null,
    )

    constructor(model: MutableList<PlaylistModel>, context: Context, listener: ListCaller) : this(
        model = model,
        context = context,
        dialogListener = null,
        listListener = listener,
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.row_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(model[position])
    }

    override fun getItemCount(): Int = model.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        @JvmField
        @BindView(R2.id.lbl_name)
        var lblName: TextView? = null

        @JvmField
        @BindView(R2.id.lbl_song_count)
        var lblSongCount: TextView? = null

        @JvmField
        @BindView(R2.id.btn_menu)
        var btnMenu: ImageView? = null

        init {
            ButterKnife.bind(this, itemView)
            itemView.setOnClickListener(this)
            btnMenu?.setOnClickListener(this)
        }

        fun bind(item: PlaylistModel) {
            lblName?.text = item.name
            val res: Resources = context.resources
            val songsFound = res.getQuantityString(
                R.plurals.playlist_messages,
                item.song_count,
                item.song_count,
            )
            lblSongCount?.text = songsFound
            if (listListener != null) {
                btnMenu?.visibility = View.INVISIBLE
            }
        }

        override fun onClick(v: View) {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) {
                return
            }
            val playlist = model[position]
            if (v.id == btnMenu?.id) {
                val popup = PopupMenu(v.context, v)
                popup.inflate(R.menu.menu_playlist)
                popup.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.playlist_edit -> {
                            PlaylistDialog(context, dialogListener).cargar(playlist)
                            true
                        }
                        R.id.playlist_delete -> {
                            delete(playlist)
                            true
                        }
                        else -> true
                    }
                }
                popup.show()
            } else {
                if (dialogListener != null) {
                    val intent = Intent(context, PlaylistDetailActivity::class.java)
                    intent.putExtra("PLAYLIST_ID", playlist.playlist_id)
                    intent.putExtra("PLAYLIST_NAME", playlist.name)
                    intent.putExtra("PLAYLIST_COUNT", playlist.song_count)
                    intent.putExtra("PLAYLIST_OFFLINE", playlist.offline)
                    context.startActivity(intent)
                }
                if (listListener != null) {
                    listListener.onListItemClick(playlist.playlist_id)
                }
            }
        }

        private fun delete(obj: PlaylistModel) {
            MaterialDialog.Builder(context)
                .title("Eliminar")
                .content("¿Desea eliminar la lista de reproducción?")
                .cancelable(false)
                .positiveText("SI")
                .onPositive(object : MaterialDialog.SingleButtonCallback {
                    override fun onClick(dialog: MaterialDialog, which: DialogAction) {
                        dao?.delete(obj)
                        dialogListener?.onPositiveCall()
                    }
                })
                .negativeText("NO")
                .show()
        }
    }
}
