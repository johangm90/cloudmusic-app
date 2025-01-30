package com.jgm90.cloudmusic.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jgm90.cloudmusic.GlideApp;
import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.R2;
import com.jgm90.cloudmusic.adapters.InfinitePagerAdapter;
import com.jgm90.cloudmusic.adapters.SlidePagerAdapter;
import com.jgm90.cloudmusic.events.IsPlayingEvent;
import com.jgm90.cloudmusic.events.OnSourceChangeEvent;
import com.jgm90.cloudmusic.events.PlayPauseEvent;
import com.jgm90.cloudmusic.interfaces.RestInterface;
import com.jgm90.cloudmusic.models.LyricModel;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.services.MediaPlayerService;
import com.jgm90.cloudmusic.utils.AnimationUtils;
import com.jgm90.cloudmusic.utils.LyricLine;
import com.jgm90.cloudmusic.utils.Lyrics;
import com.jgm90.cloudmusic.utils.PlaybackMode;
import com.jgm90.cloudmusic.utils.RestClient;
import com.jgm90.cloudmusic.utils.SharedUtils;
import com.jgm90.cloudmusic.widgets.CMViewPager;
import com.jgm90.cloudmusic.widgets.PlayPauseDrawable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NowPlayingActivity extends BaseActivity {

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.jgm90.cloudmusic.PlayNewAudio";
    public final static String SONG_OBJECT = "song_model";
    public final static String LYRICS_OBJECT = "lyrics_model";
    public static List<SongModel> audioList;
    // views
    @BindView(R2.id.pager)
    CMViewPager pager;
    @BindView(R2.id.song_title)
    public TextView songtitle;
    @BindView(R2.id.song_artist)
    public TextView songartist;
    @BindView(R2.id.song_progress)
    public SeekBar mProgress;
    @BindView(R2.id.song_elapsed_time)
    public TextView elapsedtime;
    @BindView(R2.id.song_duration)
    public TextView songduration;
    @BindView(R2.id.shuffle)
    public ImageView shuffle;
    @BindView(R2.id.previous)
    public ImageView previous;
    @BindView(R2.id.playpausefloating)
    public FloatingActionButton playPauseFloating;
    @BindView(R2.id.next)
    public ImageView next;
    @BindView(R2.id.repeat)
    public ImageView repeat;
    @BindView(R2.id.headerViewA)
    public CoordinatorLayout header_view;
    public Call<List<SongModel>> song_call;
    public Call<LyricModel> lyrics_call;
    //seekbar
    public Runnable mUpdateProgress = new Runnable() {

        @Override
        public void run() {
            if (player_service != null && player_service.isPlaying() && mProgress != null) {
                songduration.setText(SharedUtils.makeShortTimeString(getApplicationContext(), player_service.duration() / 1000));
                mProgress.setMax((int) player_service.duration());
                long position = player_service.getPosition();
                mProgress.setProgress((int) position);
                //Log.i("Progress", String.valueOf(SharedUtils.makeShortTimeString(getApplicationContext(), getPosition / 1000)));
                if (elapsedtime != null && getApplicationContext() != null)
                    elapsedtime.setText(SharedUtils.makeShortTimeString(getApplicationContext(), position / 1000));
            }
            progress_handler.postDelayed(mUpdateProgress, 50);
        }
    };
    private SlidePagerAdapter slideAdapter;
    private InfinitePagerAdapter infiniteAdapter;
    public int song_index;
    private SongModel song;
    private PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    private RelativeLayout lyricsView;
    private TextView currentLine;
    private TextView nextLine;
    private LyricModel lyricModel;
    private List<LyricLine> lyrics;
    private int current_line;
    private int lastPosition;
    public Runnable mUpdateLyrics = new Runnable() {
        @Override
        public void run() {
            if (player_service.isPlaying() && currentLine != null) {
                long position = player_service.getPosition();
                if (getApplicationContext() != null && position > 0 && lyrics != null) {
                    for (int i = 0; i < lyrics.size(); i++) {
                        int pos = (int) lyrics.get(i).getTime() * 1000;
                        if (pos <= position && current_line == 0) {
                            if (!lyrics.get(i).getLyric().equals("")) {
                                currentLine.setText(lyrics.get(i).getLyric().replace("&apos;", "'"));
                                animteLyric();
                            }
                            nextLine.setText(lyrics.get(i + 1).getLyric().replace("&apos;", "'"));
                        }
                        if (pos <= position && i > current_line) {
                            if (!lyrics.get(i).getLyric().equals("")) {
                                currentLine.setText(lyrics.get(i).getLyric().replace("&apos;", "'"));
                                animteLyric();
                            }
                            current_line = i;
                            if (i + 1 < lyrics.size()) {
                                nextLine.setText(lyrics.get(i + 1).getLyric().replace("&apos;", "'"));
                            } else {
                                nextLine.setText("");
                            }
                        }
                    }
                }
            }
            lyrics_handler.postDelayed(mUpdateLyrics, 50);
        }
    };
    private Handler main_handler, progress_handler, lyrics_handler;
    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player_service = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    private Runnable loadInfo = new Runnable() {
        @Override
        public void run() {
            getDetail(audioList.get(song_index).getId());
            getLyrics(audioList.get(song_index).getId());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);
        setupToolbar();
        ButterKnife.bind(this);
        Bundle extras = getIntent().getExtras();
        main_handler = new Handler();
        progress_handler = new Handler();
        lyrics_handler = new Handler();
        if (SharedUtils.isMyServiceRunning(this, MediaPlayerService.class)) {
            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        slideAdapter = new SlidePagerAdapter(this, audioList);
        if (SharedUtils.getRepeatMode(this) == PlaybackMode.REPEAT) {
            infiniteAdapter = new InfinitePagerAdapter(slideAdapter);
            pager.setAdapter(infiniteAdapter);
        } else {
            pager.setAdapter(slideAdapter);
        }
        if (extras != null) {
            song_index = extras.getInt("SONG_INDEX");
        } else {
            song_index = MediaPlayerService.audioIndex;
        }
        lastPosition = pager.getCurrentItem();
        pager.setCurrentItem(song_index);
        if (savedInstanceState != null) {
            song = savedInstanceState.getParcelable(SONG_OBJECT);
            lyricModel = savedInstanceState.getParcelable(LYRICS_OBJECT);
            if (song != null) {
                setDetails();
            } else {
                getDetail(audioList.get(song_index).getId());
            }
            if (lyricModel != null) {
                if (lyricModel.getLyric() != null) {
                    lyrics = Lyrics.parse(lyricModel.getLyric());
                    if (lyrics.size() > 0) {
                        lyrics_handler.postDelayed(mUpdateLyrics, 10);
                    } else {
                        currentLine.setText("No lyrics found");
                        nextLine.setText("");
                    }
                } else {
                    currentLine.setText("No lyrics found");
                    nextLine.setText("");
                }
            } else {
                getLyrics(audioList.get(song_index).getId());
            }
            lyricsView.setVisibility(View.VISIBLE);
        } else {
            if (extras != null) {
                song_index = extras.getInt("SONG_INDEX");
                playAudio();
                MediaPlayerService.audioList = audioList;
                MediaPlayerService.audioIndex = song_index;
                //main_handler.post(loadInfo);
            } else {
                song_index = MediaPlayerService.audioIndex;
                song = MediaPlayerService.song;
                setDetails();
                getLyrics(audioList.get(song_index).getId());
            }
        }
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d("last", String.valueOf(lastPosition));
                Log.d("pos", String.valueOf(position));
                if (lastPosition > position) {
                    skipToPrevious();
                } else if (lastPosition < position) {
                    skipToNext();
                }
                lastPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        EventBus.getDefault().removeStickyEvent(OnSourceChangeEvent.class);
        EventBus.getDefault().register(this);
        init();
    }

    public void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            final ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayHomeAsUpEnabled(true);
                ab.setTitle("");
            }
            toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_down_24dp);
        }
    }

    public void init() {
        if (SharedUtils.getShuffle(this)) {
            shuffle.setAlpha(1f);
        } else {
            shuffle.setAlpha(0.4f);
        }
        PlaybackMode mode = SharedUtils.getRepeatMode(this);
        switch (mode) {
            case NORMAL:
                repeat.setImageResource(R.drawable.ic_repeat_black_24dp);
                repeat.setAlpha(0.4f);
                break;
            case REPEAT:
                repeat.setImageResource(R.drawable.ic_repeat_black_24dp);
                repeat.setAlpha(1f);
                break;
            case REPEAT_ONE:
                repeat.setImageResource(R.drawable.ic_repeat_one_black_24dp);
                repeat.setAlpha(1f);
                break;
        }
    }

    private void animteLyric() {
        Animation a = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up);
        a.reset();
        currentLine.clearAnimation();
        currentLine.startAnimation(a);
    }

    @OnClick(R2.id.playpausefloating)
    public void playOrPause() {
        main_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                player_service.eventPlayOrPause(new PlayPauseEvent("From Now Playing Activity"));
            }
        }, 250);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (song != null) {
            outState.putParcelable(SONG_OBJECT, song);
        } else {
            if (song_call != null) {
                song_call.cancel();
            }
        }
        if (lyricModel != null) {
            outState.putParcelable(LYRICS_OBJECT, lyricModel);
        } else {
            if (lyrics_call != null) {
                lyrics_call.cancel();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        progress_handler.removeCallbacks(mUpdateProgress);
        lyrics_handler.removeCallbacks(mUpdateLyrics);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player_service != null && player_service.isPlaying()) {
            progress_handler.post(mUpdateProgress);
        }
        if (lyricModel != null && lyricModel.getLyric() != null) {
            lyrics_handler.post(mUpdateLyrics);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (song_call != null) {
            song_call.cancel();
        }
        progress_handler.removeCallbacks(mUpdateProgress);
        lyrics_handler.removeCallbacks(mUpdateLyrics);
        EventBus.getDefault().unregister(this);
        unbindService(serviceConnection);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void setIcon(IsPlayingEvent event) {
        if (event.isPlaying) {
            playPauseDrawable.transformToPause(true);
        } else {
            playPauseDrawable.transformToPlay(true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void setInfo(OnSourceChangeEvent event) {
        Log.d("Event", event.message);
        resetLyricsView();
        song_index = player_service.current_index();
        main_handler.post(loadInfo);
    }

    private void resetLyricsView() {
        if (currentLine != null && nextLine != null) {
            current_line = 0;
            currentLine.setText("");
            nextLine.setText("");
        }
    }

    public void getDetail(String id) {
        song = audioList.get(song_index);
        if (song != null) {
            setDetails();
        }
    }

    public void getLyrics(String id) {
        song = audioList.get(song_index);
        lyricsView = pager.findViewWithTag("lyricsView_" + song.getId());
        currentLine = pager.findViewWithTag("currentView_" + song.getId());
        nextLine = pager.findViewWithTag("nextView_" + song.getId());
        if (lyricsView != null) {
            if (!TextUtils.isEmpty(song.getLocal_lyric())) {
                lyricModel = new LyricModel(1, 1, song.getLocal_lyric(), 200);
                lyrics = Lyrics.parse(lyricModel.getLyric());
                if (lyrics != null && lyrics.size() > 0) {
                    lyrics_handler.postDelayed(mUpdateLyrics, 10);
                    lyricsView.setVisibility(View.VISIBLE);
                }
            } else {
                RestInterface api = RestClient.build(SharedUtils.server);
                lyrics_call = api.getLyrics(id);
                lyrics_call.enqueue(new Callback<LyricModel>() {
                    @Override
                    public void onResponse(Call<LyricModel> call, Response<LyricModel> response) {
                        if (response.isSuccessful()) {
                            lyricModel = response.body();
                            if (lyricModel.getLyric() != null && !lyricModel.getLyric().isEmpty()) {
                                Log.i("Lyrics", lyricModel.getLyric());
                                lyrics = Lyrics.parse(lyricModel.getLyric());
                                lyrics_handler.postDelayed(mUpdateLyrics, 10);
                            } else {
                                currentLine.setText("No lyrics found");
                                nextLine.setText("");
                            }
                            lyricsView.setVisibility(View.VISIBLE);
                        } else {
                            //RestClient.getErrorMessage(NowPlayingActivity.this, response.message());
                            Log.e("App", "Bad response");
                        }
                    }

                    @Override
                    public void onFailure(Call<LyricModel> call, Throwable t) {
                        //RestClient.getErrorMessage(NowPlayingActivity.this, t.getMessage());
                        Log.e("App", "Fail to get lyrics");
                    }
                });
            }
        }
    }

    Palette.Swatch getMostPopulousSwatch(Palette palette) {
        Palette.Swatch mostPopulous = null;
        if (palette != null) {
            for (Palette.Swatch swatch : palette.getSwatches()) {
                if (mostPopulous == null || swatch.getPopulation() > mostPopulous.getPopulation()) {
                    mostPopulous = swatch;
                }
            }
        }
        return mostPopulous;
    }

    private void setUpBackgroundColor(CoordinatorLayout fl, Palette palette) {
        Palette.Swatch swatch = getMostPopulousSwatch(palette);
        if (swatch != null) {
            int startColor = ContextCompat.getColor(fl.getContext(), R.color.grey_800);
            int endColor = swatch.getRgb();
            AnimationUtils.animColorChange(fl, startColor, endColor);
        }
    }

    public void setDetails() {
        song = audioList.get(song_index);
        String picUrl;
        if (!TextUtils.isEmpty(song.getLocal_thumbnail())) {
            picUrl = song.getLocal_thumbnail();
        } else {
            picUrl = SharedUtils.server + "pic/" + song.getPic_id();
        }
        lastPosition = song_index;
        if (SharedUtils.getShuffle(this)) {
            pager.setCurrentItem(song_index, false);
        } else {
            pager.setCurrentItem(song_index, true);
        }
        GlideApp.with(getApplicationContext())
                .asBitmap()
                .load(picUrl)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                            public void onGenerated(Palette palette) {
                                setUpBackgroundColor(header_view, palette);

                            }
                        });
                    }
                });
        songtitle.setText(song.getName());
        songtitle.setSelected(true);
        songartist.setText(TextUtils.join(", ", song.getArtist()));
        if (playPauseFloating != null) {
            //playPauseDrawable.setColorFilter(SharedUtils.getBlackWhiteColor(accentColor), PorterDuff.Mode.MULTIPLY);
            playPauseFloating.setImageDrawable(playPauseDrawable);
            //if (player_service.isPlaying())
            playPauseDrawable.transformToPause(false);
            //else playPauseDrawable.transformToPlay(false);
        }
        if (songduration != null && getApplicationContext() != null) {
            if (player_service != null && player_service.isPlaying()) {
                songduration.setText(SharedUtils.makeShortTimeString(getApplicationContext(), player_service.duration() / 1000));
            }
        }
        if (mProgress != null) {
            if (player_service != null && player_service.isPlaying()) {
                mProgress.setMax((int) player_service.duration());
            }
            if (mUpdateProgress != null) {
                progress_handler.removeCallbacks(mUpdateProgress);
            }
            progress_handler.postDelayed(mUpdateProgress, 10);
        }
        setSeekBarListener();
    }

    @OnClick(R2.id.previous)
    public void skipToPrevious() {
        main_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                player_service.skipToPrevious();
                song_index = player_service.current_index();
                main_handler.postDelayed(loadInfo, 10);
                if (SharedUtils.getRepeatMode(getApplicationContext()) != PlaybackMode.REPEAT
                        && song_index == 0) {
                    previous.setEnabled(false);
                    previous.setAlpha(0.4f);
                } else {
                    next.setEnabled(true);
                    next.setAlpha(1f);
                }
            }
        }, 200);
    }

    @OnClick(R2.id.next)
    public void skipToNext() {
        main_handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                player_service.skipToNext();
                song_index = player_service.current_index();
                main_handler.postDelayed(loadInfo, 10);
                if (SharedUtils.getRepeatMode(getApplicationContext()) != PlaybackMode.REPEAT
                        && song_index == audioList.size() - 1) {
                    next.setEnabled(false);
                    next.setAlpha(0.4f);
                } else {
                    previous.setEnabled(true);
                    previous.setAlpha(1f);
                }
            }
        }, 200);
    }

    @OnClick(R2.id.shuffle)
    public void setShuffle() {
        if (SharedUtils.getShuffle(this)) {
            shuffle.setAlpha(0.4f);
            SharedUtils.setShuffle(this, false);
        } else {
            shuffle.setAlpha(1f);
            SharedUtils.setShuffle(this, true);
        }
    }

    @OnClick(R2.id.repeat)
    public void setRepeatMode() {
        PlaybackMode mode = SharedUtils.getRepeatMode(this);
        switch (mode) {
            case NORMAL:
                repeat.setImageResource(R.drawable.ic_repeat_black_24dp);
                repeat.setAlpha(1f);
                SharedUtils.setRepeatMode(this, PlaybackMode.REPEAT);
                player_service.setLoop(false);
                break;
            case REPEAT:
                repeat.setImageResource(R.drawable.ic_repeat_one_black_24dp);
                repeat.setAlpha(1f);
                SharedUtils.setRepeatMode(this, PlaybackMode.REPEAT_ONE);
                player_service.setLoop(true);
                break;
            case REPEAT_ONE:
                repeat.setImageResource(R.drawable.ic_repeat_black_24dp);
                repeat.setAlpha(0.4f);
                SharedUtils.setRepeatMode(this, PlaybackMode.NORMAL);
                player_service.setLoop(false);
                break;
        }
    }

    private void setSeekBarListener() {
        if (mProgress != null)
            mProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if (b) {
                        resetLyricsView();
                        player_service.seek(i);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("serviceStatus");
    }

    @Override
    protected void onServiceAttached(MediaPlayerService service) {
        super.onServiceAttached(service);
        serviceBound = true;
    }

    private void playAudio() {
        if (!serviceBound) {
            startService(new Intent(this, MediaPlayerService.class));
        } else {
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            supportFinishAfterTransition();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
