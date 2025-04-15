package com.jgm90.cloudmusic.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.viewpager.widget.PagerAdapter;

import com.jgm90.cloudmusic.GlideApp;
import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.utils.SharedUtils;

import java.util.List;

public class SlidePagerAdapter extends PagerAdapter {

    private Context context;
    private LayoutInflater inflater;
    private List<SongModel> items;

    private SongModel song;

    public SlidePagerAdapter(Context context, List<SongModel> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return (view == object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.slide, container, false);
        AppCompatImageView albumArt = view.findViewById(R.id.album_art);
        RelativeLayout lyricsView = view.findViewById(R.id.lyrics_view);
        TextView currentLine = view.findViewById(R.id.lbl_currect_line);
        TextView nextLine = view.findViewById(R.id.lbl_next_line);
        albumArt.setImageResource(R.drawable.default_cover);
        String picUrl;
        song = items.get(position);
        view.setTag("rootView_" + song.getId());
        lyricsView.setTag("lyricsView_" + song.getId());
        currentLine.setTag("currentView_" + song.getId());
        nextLine.setTag("nextView_" + song.getId());
        if (!TextUtils.isEmpty(song.getLocal_thumbnail())) {
            picUrl = song.getLocal_thumbnail();
        } else {
            picUrl = SharedUtils.server + "pic/" + song.getPic_id();
        }
        GlideApp
                .with(context)
                .load(picUrl)
                .centerCrop()
                .placeholder(R.drawable.default_cover)
                .into(albumArt);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((RelativeLayout) object);
    }
}
