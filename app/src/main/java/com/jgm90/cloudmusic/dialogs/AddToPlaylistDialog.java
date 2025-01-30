package com.jgm90.cloudmusic.dialogs;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.adapters.PlaylistsAdapter;
import com.jgm90.cloudmusic.data.PlaylistData;
import com.jgm90.cloudmusic.data.SongData;
import com.jgm90.cloudmusic.interfaces.DialogCaller;
import com.jgm90.cloudmusic.interfaces.ListCaller;
import com.jgm90.cloudmusic.models.PlaylistModel;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.utils.Divider;
import com.jgm90.cloudmusic.utils.SharedUtils;

import java.util.List;

public class AddToPlaylistDialog implements ListCaller, DialogCaller {

    private Context context;
    private MaterialDialog.Builder dialog_builder;
    private MaterialDialog dialog;
    private View dv;
    private RecyclerView recyclerView;
    private PlaylistsAdapter adapter;
    private SongModel search_obj;
    private SongModel song_obj;
    private SongData dao;
    private AppCompatButton btn_add;

    public AddToPlaylistDialog(Context context) {
        this.context = context;
        this.dao = new SongData(context);
    }

    public void show(SongModel obj) {
        this.search_obj = obj;
        dialog_builder = new MaterialDialog.Builder(context)
                .title("Add to playlist")
                .customView(R.layout.dialog_add_to_playlist, false)
                .cancelable(false)
                .autoDismiss(false)
                .positiveText("")
                .negativeText("CERRAR")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                });
        dialog = dialog_builder.build();
        dialog.show();
        dv = dialog.getCustomView();
        bind();
    }

    private void bind() {
        recyclerView = dv.findViewById(R.id.rv_playlists);
        btn_add = dv.findViewById(R.id.btn_add);
        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PlaylistDialog(context, AddToPlaylistDialog.this).show();
            }
        });
        recyclerView.setAdapter(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new Divider(context));
        recyclerView.getItemAnimator().setAddDuration(SharedUtils.rv_anim_duration);
        reload();
    }

    private void reload() {
        PlaylistData dao = new PlaylistData(context);
        List<PlaylistModel> playlists = dao.getAll();
        if (playlists.size() > 0) {
            adapter = new PlaylistsAdapter(playlists, context, (ListCaller) this);
            recyclerView.setAdapter(adapter);
            adapter.notifyItemChanged(0);
        }
    }

    @Override
    public void onListItemClick(int itemId) {
        song_obj = new SongModel(
                search_obj.getId(),
                search_obj.getName(),
                search_obj.getArtist(),
                search_obj.getAlbum(),
                search_obj.getPic_id(),
                search_obj.getUrl_id(),
                search_obj.getLyric_id(),
                search_obj.getSource(),
                "",
                "",
                "",
                dao.getNextPosition(),
                SharedUtils.getDateTime(),
                itemId
        );
        dao.insert(song_obj);
        Toast.makeText(context, "Item added to playlist", Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    @Override
    public void onPositiveCall() {
        reload();
    }
}
