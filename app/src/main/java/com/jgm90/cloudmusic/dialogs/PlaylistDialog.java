package com.jgm90.cloudmusic.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.data.PlaylistData;
import com.jgm90.cloudmusic.interfaces.DialogCaller;
import com.jgm90.cloudmusic.models.PlaylistModel;

public class PlaylistDialog {

    private Context context;
    private MaterialDialog.Builder dialog_builder;
    private MaterialDialog dialog;
    private View dv;
    private DialogCaller listener;
    private EditText txt_playlist_name;
    private PlaylistModel obj;
    private PlaylistData dao;
    private boolean cargado;

    public PlaylistDialog(Context context, DialogCaller listener) {
        this.context = context;
        this.listener = listener;
        this.dao = new PlaylistData(context);
    }

    public void show() {
        dialog_builder = new MaterialDialog.Builder(context)
                .title("Playlist")
                .customView(R.layout.dialog_new_playlist, false)
                .cancelable(false)
                .autoDismiss(false)
                .positiveText("OK")
                .negativeText("CANCEL")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (validate()) {
                            guardar();
                            dialog.dismiss();
                            listener.onPositiveCall();
                        }
                    }
                })
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

    public void bind() {
        txt_playlist_name = dv.findViewById(R.id.txt_playlist_name);
        if (cargado) {
            txt_playlist_name.setText(obj.getName());
        }
    }

    public boolean validate() {
        if (txt_playlist_name.getText().toString().length() > 0) {
            return true;
        }
        return false;
    }

    public void guardar() {
        PlaylistModel item = new PlaylistModel(
                0,
                txt_playlist_name.getText().toString(),
                0
        );
        if (cargado) {
            item.setPlaylist_id(obj.getPlaylist_id());
            dao.update(item);
        } else {
            dao.insert(item);
        }
    }

    public void cargar(PlaylistModel obj) {
        this.obj = obj;
        cargado = true;
        show();
    }
}
