package com.jgm90.cloudmusic.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.R2;
import com.jgm90.cloudmusic.activities.PlaylistDetailActivity;
import com.jgm90.cloudmusic.data.PlaylistData;
import com.jgm90.cloudmusic.dialogs.PlaylistDialog;
import com.jgm90.cloudmusic.interfaces.DialogCaller;
import com.jgm90.cloudmusic.interfaces.ListCaller;
import com.jgm90.cloudmusic.models.PlaylistModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.ViewHolder> {

    private List<PlaylistModel> model;
    private LayoutInflater inflater;
    private Context context;
    private PlaylistData dao;
    private DialogCaller dialog_listener;
    private ListCaller list_listener;

    public PlaylistsAdapter(List<PlaylistModel> model, Context context, DialogCaller listener) {
        inflater = LayoutInflater.from(context);
        this.model = model;
        this.context = context;
        this.dialog_listener = listener;
        this.dao = new PlaylistData(context);
    }

    public PlaylistsAdapter(List<PlaylistModel> model, Context context, ListCaller listener) {
        inflater = LayoutInflater.from(context);
        this.model = model;
        this.context = context;
        this.list_listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.row_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(model.get(position));
    }

    @Override
    public int getItemCount() {
        return model.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R2.id.lbl_name)
        public TextView lbl_name;
        @BindView(R2.id.lbl_song_count)
        public TextView lbl_song_count;
        @BindView(R2.id.btn_menu)
        public ImageView btn_menu;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
            btn_menu.setOnClickListener(this);
        }

        public void bind(PlaylistModel item) {
            lbl_name.setText(item.getName());
            Resources res = context.getResources();
            String songsFound = res.getQuantityString(R.plurals.playlist_messages, item.getSong_count(), item.getSong_count());
            lbl_song_count.setText(songsFound);
            if (list_listener != null) {
                btn_menu.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            final PlaylistModel playlist = model.get(getAdapterPosition());
            if (v.getId() == btn_menu.getId()) {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.menu_playlist);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int itemId = item.getItemId();
                        if (itemId == R.id.playlist_edit) {
                            new PlaylistDialog(context, dialog_listener).cargar(playlist);
                            return true;
                        } else if (itemId == R.id.playlist_delete) {
                            delete(playlist);
                            return true;
                        }
                        return true;
                    }
                });
                popup.show();
            } else {
                if (dialog_listener != null) {
                    Intent intent = new Intent(context, PlaylistDetailActivity.class);
                    intent.putExtra("PLAYLIST_ID", playlist.getPlaylist_id());
                    intent.putExtra("PLAYLIST_NAME", playlist.getName());
                    intent.putExtra("PLAYLIST_COUNT", playlist.getSong_count());
                    intent.putExtra("PLAYLIST_OFFLINE", playlist.getOffline());
                    context.startActivity(intent);
                }
                if (list_listener != null) {
                    int playlist_id = playlist.getPlaylist_id();
                    list_listener.onListItemClick(playlist_id);
                }
            }
        }

        public void delete(final PlaylistModel obj) {
            new MaterialDialog.Builder(context)
                    .title("Eliminar")
                    .content("¿Desea eliminar la lista de reproducción?")
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
    }
}
