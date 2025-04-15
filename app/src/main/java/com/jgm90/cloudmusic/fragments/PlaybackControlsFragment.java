package com.jgm90.cloudmusic.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.jgm90.cloudmusic.GlideApp;
import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.activities.NowPlayingActivity;
import com.jgm90.cloudmusic.events.IsPlayingEvent;
import com.jgm90.cloudmusic.events.OnSourceChangeEvent;
import com.jgm90.cloudmusic.events.PlayPauseEvent;
import com.jgm90.cloudmusic.services.MediaPlayerService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PlaybackControlsFragment extends Fragment {

    public static String TAG = "PBC";
    private final View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            EventBus.getDefault().post(new PlayPauseEvent("From Playback Controls Fragment"));
        }
    };
    private ImageButton mPlayPause;
    private TextView mTitle;
    private TextView mSubtitle;
    private TextView mExtraInfo;
    private AppCompatImageView mAlbumArt;
    private String mArtUrl;
    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            Log.d(TAG, "Received playback state change to state");
            PlaybackControlsFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata == null) {
                return;
            }
            Log.d(TAG, "Received metadata state change to mediaId");
            PlaybackControlsFragment.this.onMetadataChanged(metadata);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false);
        mPlayPause = rootView.findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(mButtonListener);
        mTitle = rootView.findViewById(R.id.title);
        mSubtitle = rootView.findViewById(R.id.artist);
        mExtraInfo = rootView.findViewById(R.id.extra_info);
        mAlbumArt = rootView.findViewById(R.id.album_art);
        mTitle.setSelected(true);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), NowPlayingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "fragment.onStart");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            onConnected();
        }
        loadData();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "fragment.onStop");
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.unregisterCallback(mCallback);
        }
        EventBus.getDefault().unregister(this);
    }

    public void onConnected() {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        Log.d(TAG, "onConnected, mediaController==null?");
        if (controller != null) {
            onMetadataChanged(controller.getMetadata());
            onPlaybackStateChanged(controller.getPlaybackState());
            controller.registerCallback(mCallback);
        }
    }

    private void onMetadataChanged(MediaMetadataCompat metadata) {
        Log.d(TAG, "onMetadataChanged");
        if (getActivity() == null) {
            Log.w(TAG, "onMetadataChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (metadata == null) {
            return;
        }
        mTitle.setText(metadata.getDescription().getTitle());
        mSubtitle.setText(metadata.getDescription().getSubtitle());
        String artUrl = null;
        if (metadata.getDescription().getIconUri() != null) {
            artUrl = metadata.getDescription().getIconUri().toString();
        }
        if (!TextUtils.equals(artUrl, mArtUrl)) {
            mArtUrl = artUrl;
            GlideApp.with(getActivity())
                    .load(mArtUrl)
                    .placeholder(R.drawable.default_cover)
                    .centerCrop()
                    .into(mAlbumArt);
        }
    }

    public void loadData() {
        mTitle.setText(MediaPlayerService.getSongName());
        mSubtitle.setText(MediaPlayerService.getSongArtists());
        GlideApp.with(getActivity())
                .load(MediaPlayerService.getAlbumArtUrl())
                .placeholder(R.drawable.default_cover)
                .centerCrop()
                .into(mAlbumArt);
    }

    public void setExtraInfo(String extraInfo) {
        if (extraInfo == null) {
            mExtraInfo.setVisibility(View.GONE);
        } else {
            mExtraInfo.setText(extraInfo);
            mExtraInfo.setVisibility(View.VISIBLE);
        }
    }

    private void onPlaybackStateChanged(PlaybackStateCompat state) {
        Log.d(TAG, "onPlaybackStateChanged");
        if (getActivity() == null) {
            Log.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) {
            return;
        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackStateCompat.STATE_ERROR:
                Log.e(TAG, "error playbackstate");
                Toast.makeText(getActivity(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
                break;
        }
        if (enablePlay) {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow));
        } else {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause));
        }
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        String extraInfo = null;
        setExtraInfo(extraInfo);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void setIcon(IsPlayingEvent event) {
        Log.d("Event", "isPlaying " + event.isPlaying);
        if (event.isPlaying) {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause));
        } else {
            mPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void setData(OnSourceChangeEvent event) {
        Log.d("Event", "onCompletion " + event.message);
        loadData();
    }

    private void playMedia() {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pauseMedia() {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }
}
