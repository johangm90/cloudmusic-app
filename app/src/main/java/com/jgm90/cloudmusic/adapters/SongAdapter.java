package com.jgm90.cloudmusic.adapters;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.R2;
import com.jgm90.cloudmusic.activities.NowPlayingActivity;
import com.jgm90.cloudmusic.data.SongData;
import com.jgm90.cloudmusic.events.DownloadEvent;
import com.jgm90.cloudmusic.interfaces.DialogCaller;
import com.jgm90.cloudmusic.interfaces.ItemTouchHelperAdapter;
import com.jgm90.cloudmusic.interfaces.ItemTouchHelperViewHolder;
import com.jgm90.cloudmusic.interfaces.OnStartDragListener;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.utils.SharedUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> implements ItemTouchHelperAdapter {

    private final OnStartDragListener dragListener;
    private List<SongModel> model;
    private LayoutInflater inflater;
    private Context context;
    private SongData dao;
    private DialogCaller dialog_listener;
    private SongModel selectedItem;

    public SongAdapter(List<SongModel> model, Context context, DialogCaller listener, OnStartDragListener dragListener) {
        inflater = LayoutInflater.from(context);
        this.model = model;
        this.context = context;
        this.dao = new SongData(context);
        this.dialog_listener = listener;
        this.dragListener = dragListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.row_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(model.get(position));
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(model, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        if (selectedItem != null) {
            selectedItem.setPosition(toPosition + 1);
            selectedItem.setPosition_date(SharedUtils.getDateTime());
            Log.d("Item", "Position: " + String.valueOf(selectedItem.getPosition()));
            Log.d("Item", "Position DateTime: " + String.valueOf(selectedItem.getPosition_date()));
            dao.update(selectedItem);
        }
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        model.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return model.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, ItemTouchHelperViewHolder {

        @BindView(R2.id.lbl_title)
        public TextView lbl_title;
        @BindView(R2.id.lbl_subtitle)
        public TextView lbl_subtitle;
        @BindView(R2.id.btn_menu)
        public ImageView btn_menu;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
            btn_menu.setOnClickListener(this);
        }

        public void bind(SongModel item) {
            lbl_title.setText(item.getName());
            lbl_subtitle.setText(TextUtils.join(",", item.getArtist()));
        }

        @Override
        public void onClick(View v) {
            final SongModel song = model.get(getAdapterPosition());
            if (v.getId() == btn_menu.getId()) {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.menu_playlist_item);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        SongModel video = model.get(getAdapterPosition());
                        String url = SharedUtils.server + "play/" + song.getId() + "/160";
                        String filename;
                        filename = song.getName() + ".mp3";
                        filename = filename.replaceAll("\\\\|>|<|\"|\\||\\*|\\?|%|:|#|/", "");
                        int itemId = item.getItemId();
                        if (itemId == R.id.playlist_item_descargar) {
                            EventBus.getDefault().postSticky(new DownloadEvent(true, DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED, url, song.getName(), filename));
                            return true;
                        } else if (itemId == R.id.playlist_item_eliminar) {
                            delete(video);
                            return true;
                        }
                        return true;
                    }
                });
                popup.show();
            } else {
                Intent intent = new Intent(context, NowPlayingActivity.class);
                intent.putExtra("SONG_INDEX", getAdapterPosition());
                NowPlayingActivity.audioList = model;
                context.startActivity(intent);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            dragListener.onDrag(this);
            return true;
        }

        public void delete(final SongModel obj) {
            new MaterialDialog.Builder(context)
                    .title("Eliminar")
                    .content("¿Desea eliminar este item de la lista de reproducción?")
                    .cancelable(false)
                    .positiveText("SI")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dao.delete(obj);
                            dialog_listener.onPositiveCall();
                        }
                    })
                    .negativeText("NO")
                    .show();
        }

        @Override
        public void onItemSelected() {
            selectedItem = model.get(getAdapterPosition());
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }
}
