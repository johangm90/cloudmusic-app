package com.jgm90.cloudmusic.feature.playlist.presentation.adapter

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
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
import com.jgm90.cloudmusic.feature.playback.presentation.NowPlayingActivity
import com.jgm90.cloudmusic.feature.playlist.data.SongData
import com.jgm90.cloudmusic.core.event.DownloadEvent
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.DialogCaller
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.ItemTouchHelperAdapter
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.ItemTouchHelperViewHolder
import com.jgm90.cloudmusic.feature.playlist.presentation.contract.OnStartDragListener
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.util.SharedUtils
import org.greenrobot.eventbus.EventBus
import java.util.Collections

class SongAdapter(
    private val model: MutableList<SongModel>,
    private val context: Context,
    private val dialogListener: DialogCaller,
    private val dragListener: OnStartDragListener,
) : RecyclerView.Adapter<SongAdapter.ViewHolder>(), ItemTouchHelperAdapter {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val dao: SongData = SongData(context)
    private var selectedItem: SongModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.row_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(model[position])
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(model, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        selectedItem?.let {
            it.position = toPosition + 1
            it.position_date = SharedUtils.dateTime
            Log.d("Item", "Position: ${it.position}")
            Log.d("Item", "Position DateTime: ${it.position_date}")
            dao.update(it)
        }
        return true
    }

    override fun onItemDismiss(position: Int) {
        model.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemCount(): Int = model.size

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener,
        View.OnLongClickListener,
        ItemTouchHelperViewHolder {
        @JvmField
        @BindView(R2.id.lbl_title)
        var lblTitle: TextView? = null

        @JvmField
        @BindView(R2.id.lbl_subtitle)
        var lblSubtitle: TextView? = null

        @JvmField
        @BindView(R2.id.btn_menu)
        var btnMenu: ImageView? = null

        init {
            ButterKnife.bind(this, itemView)
            itemView.setOnClickListener(this)
            btnMenu?.setOnClickListener(this)
        }

        fun bind(item: SongModel) {
            lblTitle?.text = item.name
            lblSubtitle?.text = TextUtils.join(",", item.artist)
        }

        override fun onClick(v: View) {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) {
                return
            }
            val song = model[position]
            if (v.id == btnMenu?.id) {
                val popup = PopupMenu(v.context, v)
                popup.inflate(R.menu.menu_playlist_item)
                popup.setOnMenuItemClickListener { item: MenuItem ->
                    val video = model[bindingAdapterPosition]
                    val url = SharedUtils.server + "play/" + song.id + "/160"
                    var filename = song.name + ".mp3"
                    filename = filename.replace("\\\\|>|<|\"|\\||\\*|\\?|%|:|#|/".toRegex(), "")
                    when (item.itemId) {
                        R.id.playlist_item_descargar -> {
                            EventBus.getDefault().postSticky(
                                DownloadEvent(
                                    true,
                                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                                    url,
                                    song.name,
                                    filename,
                                )
                            )
                            true
                        }
                        R.id.playlist_item_eliminar -> {
                            delete(video)
                            true
                        }
                        else -> true
                    }
                }
                popup.show()
            } else {
                val intent = Intent(context, NowPlayingActivity::class.java)
                intent.putExtra("SONG_INDEX", bindingAdapterPosition)
                NowPlayingActivity.audioList = model
                context.startActivity(intent)
            }
        }

        override fun onLongClick(view: View): Boolean {
            dragListener.onDrag(this)
            return true
        }

        private fun delete(obj: SongModel) {
            MaterialDialog.Builder(context)
                .title("Eliminar")
                .content("¿Desea eliminar este item de la lista de reproducción?")
                .cancelable(false)
                .positiveText("SI")
                .onPositive(object : MaterialDialog.SingleButtonCallback {
                    override fun onClick(dialog: MaterialDialog, which: DialogAction) {
                        dao.delete(obj)
                        dialogListener.onPositiveCall()
                    }
                })
                .negativeText("NO")
                .show()
        }

        override fun onItemSelected() {
            selectedItem = model[bindingAdapterPosition]
            itemView.setBackgroundColor(Color.LTGRAY)
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
        }
    }
}
