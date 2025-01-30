package com.jgm90.cloudmusic.adapters;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.R2;
import com.jgm90.cloudmusic.activities.NowPlayingActivity;
import com.jgm90.cloudmusic.dialogs.AddToPlaylistDialog;
import com.jgm90.cloudmusic.events.DownloadEvent;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.utils.SharedUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private List<SongModel> model;
    private LayoutInflater inflater;
    private Context context;

    public SearchAdapter(List<SongModel> model, Context context) {
        inflater = LayoutInflater.from(context);
        this.model = model;
        this.context = context;
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
    public int getItemCount() {
        return model.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

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
            if (v.getId() == btn_menu.getId()) {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.menu_video);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        SongModel song = model.get(getAdapterPosition());
                        String url = SharedUtils.server + "play/" + song.getId() + "/160";
                        String filename;
                        filename = song.getName() + ".mp3";
                        filename = filename.replaceAll("\\\\|>|<|\"|\\||\\*|\\?|%|:|#|/", "");
                        int itemId = item.getItemId();
                        if (itemId == R.id.video_descargar) {
                            EventBus.getDefault().postSticky(new DownloadEvent(true, DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED, url, song.getName(), filename));
                            return true;
                        } else if (itemId == R.id.video_add_to_playlist) {
                            new AddToPlaylistDialog(context).show(song);
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
    }
}
