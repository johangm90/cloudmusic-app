package com.jgm90.cloudmusic.feature.playback.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.jgm90.cloudmusic.GlideApp
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.feature.playback.presentation.NowPlayingActivity
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.core.model.SongModel
import com.jgm90.cloudmusic.core.playback.PlaybackMode
import com.jgm90.cloudmusic.core.playback.PlaybackStatus
import com.jgm90.cloudmusic.core.util.SharedUtils
import java.io.File
import java.util.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import retrofit2.Call

class MediaPlayerService : Service(),
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnSeekCompleteListener,
    MediaPlayer.OnInfoListener,
    MediaPlayer.OnBufferingUpdateListener,
    AudioManager.OnAudioFocusChangeListener {
    private val iBinder: IBinder = LocalBinder()
    private val audioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    var currentArt: Bitmap? = null
    var call: Call<List<SongModel>>? = null
    var shouldResume = false

    private var clickCount = 0
    private lateinit var handler: Handler
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null
    private var resumePosition = 0
    private var audioManager: AudioManager? = null
    private var activeAudio: SongModel? = null
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    private var pauseIntent: PendingIntent? = null
    private var playIntent: PendingIntent? = null
    private var previousIntent: PendingIntent? = null
    private var nextIntent: PendingIntent? = null
    private var stopIntent: PendingIntent? = null
    private var audioNoisyReceiverRegistered = false
    private val eventScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var eventJobs: List<Job> = emptyList()

    private val audioNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                Log.d("Service", "Headphones disconnected.")
                if (isPlaying()) {
                    pauseMedia()
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    buildNotification(PlaybackStatus.PAUSED)
                    AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
                }
            }
        }
    }

    private val buttonHandler = Runnable {
        when (clickCount) {
            1 -> playOrPause()
            2 -> skipToNext()
            else -> skipToPrevious()
        }
        clickCount = 0
    }

    private val playNewAudio: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (audioIndex != -1 && audioIndex < audioList.size) {
                activeAudio = audioList[audioIndex]
            } else {
                stopSelf()
            }
            stopMedia()
            mediaPlayer?.reset()
            initMediaPlayer()
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    private val userActions: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d("userActions", "Received intent with action $action")
            when (action) {
                ACTION_PAUSE -> transportControls?.pause()
                ACTION_PLAY -> transportControls?.play()
                ACTION_NEXT -> transportControls?.skipToNext()
                ACTION_PREV -> transportControls?.skipToPrevious()
                ACTION_STOP -> transportControls?.stop()
                else -> Log.w("userActions", "Unknown intent ignored. Action=$action")
            }
        }
    }

    private val mediaSessionCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val keyEvent = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent != null) {
                val keyCode = keyEvent.keyCode
                if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                    if (keyEvent.action == KeyEvent.ACTION_UP && !keyEvent.isLongPress) {
                        onRemoteClick()
                    }
                    return true
                } else {
                    when (keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            onSkipToNext()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            onSkipToPrevious()
                            return true
                        }
                    }
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPlay() {
            super.onPlay()
            resumeMedia()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            buildNotification(PlaybackStatus.PLAYING)
            AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
        }

        override fun onPause() {
            super.onPause()
            pauseMedia()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            buildNotification(PlaybackStatus.PAUSED)
            AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            skipToPrevious()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            buildNotification(PlaybackStatus.PLAYING)
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            skipToNext()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerPlayNewAudio()
        val pkg = packageName
        pauseIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE,
            Intent(ACTION_PAUSE).setPackage(pkg),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        playIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE,
            Intent(ACTION_PLAY).setPackage(pkg),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        previousIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE,
            Intent(ACTION_PREV).setPackage(pkg),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        nextIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE,
            Intent(ACTION_NEXT).setPackage(pkg),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        stopIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE,
            Intent(ACTION_STOP).setPackage(pkg),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        registerUserActions()
        handler = Handler()
        eventJobs = listOf(
            AppEventBus.observe<PlayPauseEvent>(eventScope, receiveSticky = false) { eventPlayOrPause(it) },
            AppEventBus.observe<IsPlayingEvent>(eventScope) { setIcon(it) },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (audioIndex != -1 && audioIndex < audioList.size) {
                activeAudio = audioList[audioIndex]
            } else {
                stopSelf()
            }
        } catch (e: NullPointerException) {
            stopSelf()
        }
        if (!requestAudioFocus()) {
            stopSelf()
        }
        if (mediaSessionManager == null) {
            try {
                if (activeAudio != null) {
                    initMediaSession()
                    initMediaPlayer()
                    buildNotification(PlaybackStatus.PLAYING)
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
                stopSelf()
            }
        }
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder = iBinder

    override fun onUnbind(intent: Intent): Boolean {
        mediaSession?.release()
        removeNotification()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.let {
            stopMedia()
            it.release()
            removeAudioFocus()
        }
        phoneStateListener?.let { telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE) }
        removeNotification()
        unregisterReceiver(playNewAudio)
        unregisterReceiver(userActions)
        eventJobs.forEach { it.cancel() }
        eventJobs = emptyList()
        eventScope.cancel()
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
    }

    override fun onCompletion(mp: MediaPlayer) {
        val mode = SharedUtils.getRepeatMode(this)
        if (audioIndex == audioList.size - 1) {
            if (mode == PlaybackMode.REPEAT) {
                mediaPlayer?.isLooping = false
                audioIndex = 0
                activeAudio = audioList[audioIndex]
            } else if (mode == PlaybackMode.REPEAT_ONE) {
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
                return
            } else {
                mediaPlayer?.stop()
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                buildNotification(PlaybackStatus.PAUSED)
                AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
                return
            }
        } else {
            if (mode != PlaybackMode.REPEAT_ONE) {
                mediaPlayer?.isLooping = false
                activeAudio = audioList[++audioIndex]
            } else {
                mediaPlayer?.isLooping = true
                activeAudio = audioList[audioIndex]
                mediaPlayer?.start()
                return
            }
        }
        if (SharedUtils.getShuffle(this)) {
            val random = Random()
            audioIndex = random.nextInt((audioList.size - 1) + 1)
        }
        stopMedia()
        mediaPlayer?.reset()
        initMediaPlayer()
        buildNotification(PlaybackStatus.PLAYING)
    }

    fun setLoop(loop: Boolean) {
        mediaPlayer?.isLooping = loop
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.e("Error", "$what $extra")
        when (what) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK ->
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra")
            MediaPlayer.MEDIA_ERROR_SERVER_DIED ->
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED $extra")
            MediaPlayer.MEDIA_ERROR_UNKNOWN ->
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN $extra")
        }
        return false
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean = false

    override fun onPrepared(mp: MediaPlayer) {
        Log.d("GOHAN", "MP prepared")
        playMedia()
    }

    override fun onSeekComplete(mp: MediaPlayer) {
    }

    override fun onAudioFocusChange(focusState: Int) {
        when (focusState) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mediaPlayer == null) initMediaPlayer()
                else if (shouldResume) {
                    mediaPlayer?.start()
                    AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
                    shouldResume = false
                }
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (isPlaying()) {
                    mediaPlayer?.pause()
                    AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
                    shouldResume = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying()) {
                    mediaPlayer?.pause()
                    AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
                    shouldResume = true
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.setVolume(0.1f, 0.1f)
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager?.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun removeAudioFocus(): Boolean {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager?.abandonAudioFocus(this)
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        mediaPlayer?.apply {
            setOnCompletionListener(this@MediaPlayerService)
            setOnErrorListener(this@MediaPlayerService)
            setOnPreparedListener(this@MediaPlayerService)
            setOnBufferingUpdateListener(this@MediaPlayerService)
            setOnSeekCompleteListener(this@MediaPlayerService)
            setOnInfoListener(this@MediaPlayerService)
            reset()
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
        }
        activeAudio?.let {
            val id = it.id ?: return@let
            try {
                currentIndex = -1
                val source = if (!TextUtils.isEmpty(it.local_file)) {
                    val localFile = requireNotNull(it.local_file)
                    val songFile = File(localFile)
                    if (songFile.exists()) localFile else SharedUtils.server + "play/" + id + "/160"
                } else {
                    SharedUtils.server + "play/" + id + "/160"
                }
                Log.d("MediaPlayer", source)
                mediaPlayer?.setDataSource(source)
                mediaPlayer?.prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    fun playMedia() {
        registerAudioNoisyReceiver()
        if (mediaPlayer?.isPlaying != true) {
            mediaPlayer?.start()
        }
    }

    fun playOrPause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.start()
                }
                AppEventBus.postSticky(IsPlayingEvent(it.isPlaying))
            }
        } catch (_: Exception) {
        }
    }

    private fun stopMedia() {
        unregisterAudioNoisyReceiver()
        if (mediaPlayer == null) return
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
    }

    fun pauseMedia() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    private fun resumeMedia() {
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun eventPlayOrPause(event: PlayPauseEvent) {
        Log.d("Event", event.message)
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.start()
                }
                AppEventBus.postSticky(IsPlayingEvent(it.isPlaying))
            }
        } catch (_: Exception) {
        }
    }

    fun getPosition(): Long {
        return mediaPlayer?.let {
            try {
                it.currentPosition.toLong()
            } catch (_: Exception) {
                0L
            }
        } ?: 0L
    }

    fun duration(): Long {
        return mediaPlayer?.let {
            try {
                it.duration.toLong()
            } catch (_: Exception) {
                0L
            }
        } ?: 0L
    }

    fun current_index(): Int {
        return if (mediaPlayer != null) audioIndex else -1
    }

    fun seek(position: Int) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.seekTo(position)
            } catch (_: Exception) {
            }
        }
    }

    fun skipToNext() {
        if (audioIndex == audioList.size - 1) {
            audioIndex = 0
            activeAudio = audioList[audioIndex]
        } else {
            activeAudio = audioList[++audioIndex]
        }
        if (SharedUtils.getShuffle(this)) {
            val random = Random()
            audioIndex = random.nextInt((audioList.size - 1) + 1)
            activeAudio = audioList[audioIndex]
        }
        stopMedia()
        mediaPlayer?.reset()
        initMediaPlayer()
        buildNotification(PlaybackStatus.PLAYING)
    }

    fun skipToPrevious() {
        if (audioIndex == 0) {
            audioIndex = audioList.size - 1
            activeAudio = audioList[audioIndex]
        } else {
            activeAudio = audioList[--audioIndex]
        }
        if (SharedUtils.getShuffle(this)) {
            val random = Random()
            audioIndex = random.nextInt((audioList.size - 1) + 1)
            activeAudio = audioList[audioIndex]
        }
        stopMedia()
        mediaPlayer?.reset()
        initMediaPlayer()
        buildNotification(PlaybackStatus.PLAYING)
    }

    private fun callStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK,
                    TelephonyManager.CALL_STATE_RINGING -> {
                        if (mediaPlayer != null) {
                            ongoingCall = true
                            if (isPlaying()) {
                                pauseMedia()
                                AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
                                shouldResume = true
                            }
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (mediaPlayer != null && ongoingCall) {
                            ongoingCall = false
                            if (shouldResume) {
                                resumeMedia()
                                AppEventBus.postSticky(IsPlayingEvent(mediaPlayer?.isPlaying == true))
                                shouldResume = false
                            }
                        }
                    }
                }
            }
        }
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Throws(RemoteException::class)
    private fun initMediaSession() {
        val receiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mediaSession = MediaSessionCompat(applicationContext, "MediaPlayerService", receiver, null)
        mediaSession?.setCallback(mediaSessionCallback)
        mediaSession?.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE)
        mediaSession?.setMediaButtonReceiver(pendingIntent)
        if (!retrievedAudioFocus()) {
            Log.w("App", "Failed retrieving audio focus on init media session")
            return
        }
        mediaSession?.isActive = true
        setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        transportControls = mediaSession?.controller?.transportControls
    }

    private fun setMediaPlaybackState(state: Int) {
        val playbackstateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY)
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mediaSession?.setPlaybackState(playbackstateBuilder.build())
    }

    fun setIcon(event: IsPlayingEvent) {
        if (event.isPlaying) {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            buildNotification(PlaybackStatus.PLAYING)
        } else {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            buildNotification(PlaybackStatus.PAUSED)
        }
    }

    private fun retrievedAudioFocus(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN,
        )
        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    private fun updateMetaData(albumArt: Bitmap) {
        activeAudio?.let { audio ->
            mediaSession?.setMetadata(
                MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, TextUtils.join(", ", audio.artist))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, audio.album)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, audio.name)
                    .build(),
            )
        }
    }

    private fun createContentIntent(): PendingIntent {
        val openUI = Intent(this, NowPlayingActivity::class.java)
        openUI.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE,
            openUI,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildNotification(playbackStatus: PlaybackStatus) {
        call?.cancel()
        val notification = createNotification(playbackStatus)
        if (notification != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotification(playbackStatus: PlaybackStatus): Notification? {
        val active = requireNotNull(activeAudio)
        var notificationAction = R.drawable.ic_pause
        var playPauseAction: PendingIntent? = null
        var action = "pause"
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = R.drawable.ic_pause
            playPauseAction = pauseIntent
            action = "pause"
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = R.drawable.ic_play_arrow
            playPauseAction = playIntent
            action = "play"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("100", "playback", NotificationManager.IMPORTANCE_LOW)
            channel.description = "CloudMusic channel"
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0)
            channel.enableLights(false)
            notificationManager.createNotificationChannel(channel)
        }
        val notificationBuilder = NotificationCompat.Builder(this, "100")
            .setShowWhen(false)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setVibrate(longArrayOf(0))
            .setContentIntent(createContentIntent())
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setSmallIcon(R.drawable.ic_logo)
            .setContentText(TextUtils.join(", ", active.artist))
            .setContentTitle(active.name)
            .addAction(R.drawable.ic_skip_previous, "previous", previousIntent)
            .addAction(notificationAction, action, playPauseAction)
            .addAction(R.drawable.ic_skip_next, "next", nextIntent)
        if (currentIndex != audioIndex) {
            getAlbumArt(notificationBuilder)
        } else {
            currentArt?.let { notificationBuilder.setLargeIcon(it) }
        }
        return notificationBuilder.build()
    }

    fun getAlbumArt(builder: NotificationCompat.Builder) {
        try {
            song = activeAudio
            val active = requireNotNull(song)
            val picUrl = if (!TextUtils.isEmpty(active.local_thumbnail)) {
                active.local_thumbnail
            } else {
                SharedUtils.server + "pic/" + active.pic_id
            }
            GlideApp.with(this)
                .asBitmap()
                .load(picUrl)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        builder.setLargeIcon(resource)
                        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .notify(NOTIFICATION_ID, builder.build())
                        updateMetaData(resource)
                        currentIndex = audioIndex
                        currentArt = resource
                        AppEventBus.postSticky(OnSourceChangeEvent("Metadata changed"))
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "InlinedApi")
    private fun registerPlayNewAudio() {
        val filter = IntentFilter(NowPlayingActivity.Broadcast_PLAY_NEW_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(playNewAudio, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(playNewAudio, filter)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "InlinedApi")
    private fun registerUserActions() {
        val filter = IntentFilter()
        filter.addAction(ACTION_NEXT)
        filter.addAction(ACTION_PAUSE)
        filter.addAction(ACTION_PLAY)
        filter.addAction(ACTION_PREV)
        filter.addAction(ACTION_STOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(userActions, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(userActions, filter)
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.let {
            try {
                it.isPlaying
            } catch (_: Exception) {
                false
            }
        } ?: false
    }

    private fun registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter)
            audioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            unregisterReceiver(audioNoisyReceiver)
            audioNoisyReceiverRegistered = false
        }
    }

    private fun onRemoteClick() {
        clickCount++
        handler.removeCallbacks(buttonHandler)
        handler.postDelayed(buttonHandler, REMOTE_CLICK_SLEEP_TIME_MS.toLong())
    }

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    companion object {
        const val ACTION_PLAY = "com.jgm90.cloudmusic.play"
        const val ACTION_PAUSE = "com.jgm90.cloudmusic.pause"
        const val ACTION_PREV = "com.jgm90.cloudmusic.prev"
        const val ACTION_NEXT = "com.jgm90.cloudmusic.next"
        const val ACTION_STOP = "com.jgm90.cloudmusic.stop"

        private const val REMOTE_CLICK_SLEEP_TIME_MS = 300
        private const val REQUEST_CODE = 100
        private const val NOTIFICATION_ID = 101

        @JvmField
        var audioList: MutableList<SongModel> = mutableListOf()

        @JvmField
        var audioIndex = -1

        @JvmField
        var currentIndex = -1

        @JvmField
        var song: SongModel? = null

        @JvmStatic
        fun getSongName(): String {
            return song?.name ?: ""
        }

        @JvmStatic
        fun getSongArtists(): String {
            return song?.let { TextUtils.join(", ", it.artist) } ?: ""
        }

        @JvmStatic
        fun getAlbumArtUrl(): String {
            val current = song ?: return ""
            return if (!TextUtils.isEmpty(current.local_thumbnail)) {
                current.local_thumbnail ?: ""
            } else {
                SharedUtils.server + "pic/" + current.pic_id
            }
        }
    }
}
