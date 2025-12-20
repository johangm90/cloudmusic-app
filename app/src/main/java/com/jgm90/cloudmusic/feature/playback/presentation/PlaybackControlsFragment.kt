package com.jgm90.cloudmusic.feature.playback.presentation

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.jgm90.cloudmusic.GlideApp
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.feature.playback.presentation.NowPlayingActivity
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PlaybackControlsFragment : Fragment() {
    private val buttonListener = View.OnClickListener {
        EventBus.getDefault().post(PlayPauseEvent("From Playback Controls Fragment"))
    }

    private lateinit var playPause: ImageButton
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var extraInfo: TextView
    private lateinit var albumArt: AppCompatImageView
    private var artUrl: String? = null

    private val callback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            Log.d(TAG, "Received playback state change to state")
            this@PlaybackControlsFragment.onPlaybackStateChanged(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (metadata == null) {
                return
            }
            Log.d(TAG, "Received metadata state change to mediaId")
            this@PlaybackControlsFragment.onMetadataChanged(metadata)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_playback_controls, container, false)
        playPause = rootView.findViewById(R.id.play_pause)
        playPause.isEnabled = true
        playPause.setOnClickListener(buttonListener)
        title = rootView.findViewById(R.id.title)
        subtitle = rootView.findViewById(R.id.artist)
        extraInfo = rootView.findViewById(R.id.extra_info)
        albumArt = rootView.findViewById(R.id.album_art)
        title.isSelected = true
        rootView.setOnClickListener {
            val intent = Intent(activity, NowPlayingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        return rootView
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "fragment.onStart")
        val controller = MediaControllerCompat.getMediaController(activity)
        if (controller != null) {
            onConnected()
        }
        loadData()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "fragment.onStop")
        val controller = MediaControllerCompat.getMediaController(activity)
        controller?.unregisterCallback(callback)
        EventBus.getDefault().unregister(this)
    }

    private fun onConnected() {
        val controller = MediaControllerCompat.getMediaController(activity)
        Log.d(TAG, "onConnected, mediaController==null?")
        if (controller != null) {
            onMetadataChanged(controller.metadata)
            onPlaybackStateChanged(controller.playbackState)
            controller.registerCallback(callback)
        }
    }

    private fun onMetadataChanged(metadata: MediaMetadataCompat) {
        Log.d(TAG, "onMetadataChanged")
        val host = activity ?: run {
            Log.w(TAG, "onMetadataChanged called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.")
            return
        }
        title.text = metadata.description.title
        subtitle.text = metadata.description.subtitle
        val newArtUrl = metadata.description.iconUri?.toString()
        if (!TextUtils.equals(newArtUrl, artUrl)) {
            artUrl = newArtUrl
            GlideApp.with(host)
                .load(artUrl)
                .placeholder(R.drawable.default_cover)
                .centerCrop()
                .into(albumArt)
        }
    }

    private fun loadData() {
        title.text = MediaPlayerService.getSongName()
        subtitle.text = MediaPlayerService.getSongArtists()
        GlideApp.with(this)
            .load(MediaPlayerService.getAlbumArtUrl())
            .placeholder(R.drawable.default_cover)
            .centerCrop()
            .into(albumArt)
    }

    fun setExtraInfo(extraInfo: String?) {
        if (extraInfo == null) {
            this.extraInfo.visibility = View.GONE
        } else {
            this.extraInfo.text = extraInfo
            this.extraInfo.visibility = View.VISIBLE
        }
    }

    private fun onPlaybackStateChanged(state: PlaybackStateCompat) {
        Log.d(TAG, "onPlaybackStateChanged")
        if (activity == null) {
            Log.w(TAG, "onPlaybackStateChanged called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring.")
            return
        }
        var enablePlay = false
        when (state.state) {
            PlaybackStateCompat.STATE_PAUSED,
            PlaybackStateCompat.STATE_STOPPED -> enablePlay = true
            PlaybackStateCompat.STATE_ERROR -> {
                Log.e(TAG, "error playbackstate")
                Toast.makeText(activity, state.errorMessage, Toast.LENGTH_LONG).show()
            }
        }
        if (enablePlay) {
            playPause.setImageDrawable(
                ContextCompat.getDrawable(activity, R.drawable.ic_play_arrow),
            )
        } else {
            playPause.setImageDrawable(
                ContextCompat.getDrawable(activity, R.drawable.ic_pause),
            )
        }
        setExtraInfo(null)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun setIcon(event: IsPlayingEvent) {
        Log.d("Event", "isPlaying ${event.isPlaying}")
        if (event.isPlaying) {
            playPause.setImageDrawable(
                ContextCompat.getDrawable(activity, R.drawable.ic_pause),
            )
        } else {
            playPause.setImageDrawable(
                ContextCompat.getDrawable(activity, R.drawable.ic_play_arrow),
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun setData(event: OnSourceChangeEvent) {
        Log.d("Event", "onCompletion ${event.message}")
        loadData()
    }

    companion object {
        const val TAG = "PBC"
    }
}
