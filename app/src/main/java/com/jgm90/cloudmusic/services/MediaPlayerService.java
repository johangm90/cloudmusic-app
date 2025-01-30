package com.jgm90.cloudmusic.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.jgm90.cloudmusic.GlideApp;
import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.activities.NowPlayingActivity;
import com.jgm90.cloudmusic.events.IsPlayingEvent;
import com.jgm90.cloudmusic.events.OnSourceChangeEvent;
import com.jgm90.cloudmusic.events.PlayPauseEvent;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.utils.PlaybackMode;
import com.jgm90.cloudmusic.utils.PlaybackStatus;
import com.jgm90.cloudmusic.utils.SharedUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;
import java.util.Random;

import retrofit2.Call;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "com.jgm90.cloudmusic.play";
    public static final String ACTION_PAUSE = "com.jgm90.cloudmusic.pause";
    public static final String ACTION_PREV = "com.jgm90.cloudmusic.prev";
    public static final String ACTION_NEXT = "com.jgm90.cloudmusic.next";
    public static final String ACTION_STOP = "com.jgm90.cloudmusic.stop";

    private static final int REMOTE_CLICK_SLEEP_TIME_MS = 300;
    //
    private static final int REQUEST_CODE = 100;
    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;
    //List of available Audio files
    public static List<SongModel> audioList;
    public static int audioIndex = -1;
    public static int currentIndex = -1;
    public static SongModel song;
    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();
    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    public Bitmap currentArt;
    public Call<List<SongModel>> call;
    public boolean shouldResume;
    private int mClickCount;
    private Handler mHandler;
    private MediaPlayer mediaPlayer;
    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;
    //Used to pause/resume MediaPlayer
    private int resumePosition;
    //AudioFocus
    private AudioManager audioManager;
    private SongModel activeAudio; //an object on the currently playing audio
    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private PendingIntent mPauseIntent;
    private PendingIntent mPlayIntent;
    private PendingIntent mPreviousIntent;
    private PendingIntent mNextIntent;
    private final BroadcastReceiver mAudioNoisyReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                        Log.d("Service", "Headphones disconnected.");
                        if (isPlaying()) {
                            pauseMedia();
                            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                            buildNotification(PlaybackStatus.PAUSED);
                            EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
                        }
                    }
                }
            };
    private PendingIntent mStopIntent;
    private boolean mAudioNoisyReceiverRegistered;
    private final Runnable mButtonHandler = new Runnable() {
        @Override
        public void run() {
            if (mClickCount == 1) {
                playOrPause();
            } else if (mClickCount == 2) {
                skipToNext();
            } else {
                skipToPrevious();
            }
            mClickCount = 0;
        }
    };
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Get the new media index form SharedPreferences
            //audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            //updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };
    private BroadcastReceiver userActions = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("userActions", "Received intent with action " + action);
            switch (action) {
                case ACTION_PAUSE:
                    transportControls.pause();
                    break;
                case ACTION_PLAY:
                    transportControls.play();
                    break;
                case ACTION_NEXT:
                    transportControls.skipToNext();
                    break;
                case ACTION_PREV:
                    transportControls.skipToPrevious();
                    break;
                case ACTION_STOP:
                    transportControls.stop();
                    break;
                default:
                    Log.w("userActions", "Unknown intent ignored. Action=" + action);
            }
        }
    };
    //media session callbacks
    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            int keyCode = keyEvent.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                if (keyEvent.getAction() == KeyEvent.ACTION_UP && !keyEvent.isLongPress()) {
                    onRemoteClick();
                }
                return true;
            } else {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        onSkipToNext();
                        return true;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        onSkipToPrevious();
                        return true;
                }
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        }

        @Override
        public void onPlay() {
            super.onPlay();
            resumeMedia();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            buildNotification(PlaybackStatus.PLAYING);
            EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
        }

        @Override
        public void onPause() {
            super.onPause();
            pauseMedia();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            buildNotification(PlaybackStatus.PAUSED);
            EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            skipToPrevious();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            buildNotification(PlaybackStatus.PLAYING);
            //EventBus.getDefault().postSticky(new OnSourceChangeEvent("Event from notification"));
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            skipToNext();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            buildNotification(PlaybackStatus.PLAYING);
            //EventBus.getDefault().postSticky(new OnSourceChangeEvent("Event from notification"));
        }
    };

    public static String getSongName() {
        if (song != null) {
            return song.getName();
        }
        return "";
    }

    public static String getSongArtists() {
        if (song != null) {
            return TextUtils.join(", ", song.getArtist());
        }
        return "";
    }

    public static String getAlbumArtUrl() {
        if (song != null) {
            String picUrl;
            if (!TextUtils.isEmpty(song.getLocal_thumbnail())) {
                picUrl = song.getLocal_thumbnail();
            } else {
                picUrl = SharedUtils.server + "pic/" + song.getPic_id();
            }
            return picUrl;
        }
        return "";
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            this.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            this.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    private void onRemoteClick() {
        mClickCount++;
        mHandler.removeCallbacks(mButtonHandler);
        mHandler.postDelayed(mButtonHandler, REMOTE_CLICK_SLEEP_TIME_MS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //callStateListener();
        register_playNewAudio();
        String pkg = this.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mPlayIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mPreviousIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mNextIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mStopIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
                new Intent(ACTION_STOP).setPackage(pkg),
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        register_userActions();
        mHandler = new Handler();
        EventBus.getDefault().register(this);
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }
        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf();
        }
        if (mediaSessionManager == null) {
            try {
                if (activeAudio != null) {
                    initMediaSession();
                    initMediaPlayer();
                    buildNotification(PlaybackStatus.PLAYING);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
        }
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mediaSession != null) {
            mediaSession.release();
            removeNotification();
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
            removeAudioFocus();
        }
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        removeNotification();
        //unregister BroadcastReceivers
        unregisterReceiver(playNewAudio);
        unregisterReceiver(userActions);
        //clear cached playlist
        //new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        PlaybackMode mode = SharedUtils.getRepeatMode(this);
        if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            if (mode == PlaybackMode.REPEAT) {
                mediaPlayer.setLooping(false);
                audioIndex = 0;
                activeAudio = audioList.get(audioIndex);
            } else if (mode == PlaybackMode.REPEAT_ONE) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
                return;
            } else {
                mediaPlayer.stop();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                buildNotification(PlaybackStatus.PAUSED);
                EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
                return;
            }
        } else {
            //get next in playlist
            if (mode != PlaybackMode.REPEAT_ONE) {
                mediaPlayer.setLooping(false);
                activeAudio = audioList.get(++audioIndex);
            } else {
                mediaPlayer.setLooping(true);
                activeAudio = audioList.get(audioIndex);
                mediaPlayer.start();
                return;
            }
        }
        //Update stored index
        //new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
        if (SharedUtils.getShuffle(this)) {
            Random random = new Random();
            audioIndex = random.nextInt((audioList.size() - 1) + 1);
        }
        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
        //updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);
        //EventBus.getDefault().postSticky(new OnSourceChangeEvent("Event from service"));
    }

    public void setLoop(boolean loop) {
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(loop);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        Log.e("Error", String.valueOf(what) + " " + String.valueOf(extra));
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        Log.d("GOHAN", "MP prepared");
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (shouldResume) {
                    mediaPlayer.start();
                    EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
                    shouldResume = false;
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (isPlaying()) {
                    mediaPlayer.pause();
                    EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
                    shouldResume = false;
                }
                //mediaPlayer.release();
                //mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (isPlaying()) {
                    mediaPlayer.pause();
                    EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
                    shouldResume = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null)
            mediaPlayer = new MediaPlayer();//new MediaPlayer instance
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
        if (activeAudio != null) {
            try {
                currentIndex = -1;
                // Set the data source to the mediaFile location
                String source;
                if (!TextUtils.isEmpty(activeAudio.getLocal_file())) {
                    File song = new File(activeAudio.getLocal_file());
                    if (song.exists()) {
                        source = activeAudio.getLocal_file();
                    } else {
                        source = SharedUtils.server + "play/" + activeAudio.getId() + "/" + "160";
                    }
                } else {
                    source = SharedUtils.server + "play/" + activeAudio.getId() + "/" + "160";
                }
                Log.d("MediaPlayer", source);
                mediaPlayer.setDataSource(source);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
                stopSelf();
            }
        }
    }

    public void playMedia() {
        registerAudioNoisyReceiver();
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void playOrPause() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
                EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
            }
        } catch (final Exception ignored) {
        }
    }

    private void stopMedia() {
        unregisterAudioNoisyReceiver();
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            //resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            //mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventPlayOrPause(PlayPauseEvent event) {
        Log.d("Event", event.message);
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
                EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
            }
        } catch (final Exception ignored) {
        }
    }

    public long getPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (final Exception ignored) {
            }
        }
        return 0;
    }

    public long duration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (final Exception ignored) {
            }
        }
        return 0;
    }

    public int current_index() {
        if (mediaPlayer != null) {
            return audioIndex;
        }
        return -1;
    }

    public void seek(final int position) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(position);
            } catch (Exception ignored) {
            }
        }
    }

    public void skipToNext() {
        if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get next in playlist
            activeAudio = audioList.get(++audioIndex);
        }
        if (SharedUtils.getShuffle(this)) {
            Random random = new Random();
            audioIndex = random.nextInt((audioList.size() - 1) + 1);
            activeAudio = audioList.get(audioIndex);
        }
        //Update stored index
        //new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
        //updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);
    }

    public void skipToPrevious() {
        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
        }
        if (SharedUtils.getShuffle(this)) {
            Random random = new Random();
            audioIndex = random.nextInt((audioList.size() - 1) + 1);
            activeAudio = audioList.get(audioIndex);
        }
        //Update stored index
        //new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
        //updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);
    }

    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            ongoingCall = true;
                            if (isPlaying()) {
                                pauseMedia();
                                EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
                                shouldResume = true;
                            }
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                if (shouldResume) {
                                    resumeMedia();
                                    EventBus.getDefault().postSticky(new IsPlayingEvent(mediaPlayer.isPlaying()));
                                    shouldResume = false;
                                }
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void initMediaSession() throws RemoteException {
        //ComponentName receiver = new ComponentName(getPackageName(), RemoteReceiver.class.getName());
        ComponentName receiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "MediaPlayerService", receiver, null);
        mediaSession.setCallback(mediaSessionCallback);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE);
        mediaSession.setMediaButtonReceiver(pendingIntent);
        if (!retrievedAudioFocus()) {
            Log.w("App", "Failed retrieving audio focus on init media session");
            return;
        }
        mediaSession.setActive(true);
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
        transportControls = mediaSession.getController().getTransportControls();
    }

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mediaSession.setPlaybackState(playbackstateBuilder.build());
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void setIcon(IsPlayingEvent event) {
        if (event.isPlaying) {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            buildNotification(PlaybackStatus.PLAYING);
        } else {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            buildNotification(PlaybackStatus.PAUSED);
        }
    }

    private boolean retrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    private void updateMetaData(Bitmap albumArt) {
        if (activeAudio != null) {
            // Update the current metadata
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, TextUtils.join(", ", activeAudio.getArtist()))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getName())
                    .build());
        }
    }

    private PendingIntent createContentIntent() {
        Intent openUI = new Intent(this, NowPlayingActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, REQUEST_CODE, openUI,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void buildNotification(PlaybackStatus playbackStatus) {
        if (call != null) {
            call.cancel();
        }
        Notification notification = createNotification(playbackStatus);
        if (notification != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        }
    }

    private Notification createNotification(PlaybackStatus playbackStatus) {
        int notificationAction = R.drawable.ic_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;
        String action = "pause";
        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = R.drawable.ic_pause;
            play_pauseAction = mPauseIntent;
            action = "pause";
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = R.drawable.ic_play_arrow;
            play_pauseAction = mPlayIntent;
            action = "play";
        }
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("100",
                    "playback",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("CloudMusic channel");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0});
            channel.enableLights(false);
            notificationManager.createNotificationChannel(channel);
        }
        // Create a new Notification
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "100")
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0})
                .setContentIntent(createContentIntent())
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_logo)
                // Set Notification content information
                .setContentText(TextUtils.join(", ", activeAudio.getArtist()))
                .setContentTitle(activeAudio.getName())
                //notification actions
                .addAction(R.drawable.ic_skip_previous, "previous", mPreviousIntent)
                .addAction(notificationAction, action, play_pauseAction)
                .addAction(R.drawable.ic_skip_next, "next", mNextIntent);
        if (currentIndex != audioIndex) {
            getAlbumArt(notificationBuilder);
        } else {
            if (currentArt != null) {
                notificationBuilder.setLargeIcon(currentArt);
            }
        }
        Notification notification;
        notification = notificationBuilder.build();
        return notification;
    }

    public void getAlbumArt(final NotificationCompat.Builder builder) {
        try {
            song = activeAudio;
            String picUrl;
            if (!TextUtils.isEmpty(song.getLocal_thumbnail())) {
                picUrl = song.getLocal_thumbnail();
            } else {
                picUrl = SharedUtils.server + "pic/" + song.getPic_id();
            }
            GlideApp.with(this)
                    .asBitmap()
                    .load(picUrl)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            builder.setLargeIcon(resource);
                            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, builder.build());
                            updateMetaData(resource);
                            currentIndex = audioIndex;
                            currentArt = resource;
                            EventBus.getDefault().postSticky(new OnSourceChangeEvent("Metadata changed"));
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "InlinedApi"})
    private void register_playNewAudio() {
        IntentFilter filter = new IntentFilter(NowPlayingActivity.Broadcast_PLAY_NEW_AUDIO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(playNewAudio, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(playNewAudio, filter);
        }
    }

    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "InlinedApi"})
    private void register_userActions() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(userActions, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(userActions, filter);
        }
    }

    public final boolean isPlaying() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.isPlaying();
            } catch (final Exception ignored) {
            }
        }
        return false;
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MediaPlayerService.this;
        }
    }
}
