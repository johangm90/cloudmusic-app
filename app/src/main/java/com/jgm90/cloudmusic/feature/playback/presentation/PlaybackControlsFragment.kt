package com.jgm90.cloudmusic.feature.playback.presentation

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jgm90.cloudmusic.GlideApp
import com.jgm90.cloudmusic.R
import com.jgm90.cloudmusic.databinding.FragmentPlaybackControlsBinding
import com.jgm90.cloudmusic.core.event.AppEventBus
import com.jgm90.cloudmusic.core.event.IsPlayingEvent
import com.jgm90.cloudmusic.core.event.OnSourceChangeEvent
import com.jgm90.cloudmusic.core.event.PlayPauseEvent
import com.jgm90.cloudmusic.feature.playback.service.MediaPlayerService
import kotlinx.coroutines.Job

class PlaybackControlsFragment : Fragment() {
    private val buttonListener = View.OnClickListener {
        AppEventBus.post(PlayPauseEvent("From Playback Controls Fragment"))
    }

    private var _binding: FragmentPlaybackControlsBinding? = null
    private val binding get() = _binding!!
    private lateinit var playPause: ImageButton
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var extraInfo: TextView
    private lateinit var albumArt: AppCompatImageView
    private var artUrl: String? = null
    private var eventJobs: List<Job> = emptyList()

    private val callback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
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
        _binding = FragmentPlaybackControlsBinding.inflate(inflater, container, false)
        val rootView = binding.root
        playPause = binding.playPause
        playPause.isEnabled = true
        playPause.setOnClickListener(buttonListener)
        title = binding.title
        subtitle = binding.artist
        extraInfo = binding.extraInfo
        albumArt = binding.albumArt
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
        val controller = MediaControllerCompat.getMediaController(requireActivity())
        if (controller != null) {
            onConnected()
        }
        loadData()
        eventJobs = listOf(
            AppEventBus.observe<IsPlayingEvent>(requireActivity().lifecycleScope) { setIcon(it) },
            AppEventBus.observe<OnSourceChangeEvent>(requireActivity().lifecycleScope) { setData(it) },
        )
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "fragment.onStop")
        val controller = MediaControllerCompat.getMediaController(requireActivity())
        controller?.unregisterCallback(callback)
        eventJobs.forEach { it.cancel() }
        eventJobs = emptyList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onConnected() {
        val controller = MediaControllerCompat.getMediaController(requireActivity())
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
            Log.w(
                TAG,
                "onMetadataChanged called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring."
            )
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
            Log.w(
                TAG,
                "onPlaybackStateChanged called when getActivity null, this should not happen if the callback was properly unregistered. Ignoring."
            )
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
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_arrow),
            )
        } else {
            playPause.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause),
            )
        }
        setExtraInfo(null)
    }

    fun setIcon(event: IsPlayingEvent) {
        Log.d("Event", "isPlaying ${event.isPlaying}")
        if (event.isPlaying) {
            playPause.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause),
            )
        } else {
            playPause.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_arrow),
            )
        }
    }

    fun setData(event: OnSourceChangeEvent) {
        Log.d("Event", "onCompletion ${event.message}")
        loadData()
    }

    companion object {
        const val TAG = "PBC"
    }
}
