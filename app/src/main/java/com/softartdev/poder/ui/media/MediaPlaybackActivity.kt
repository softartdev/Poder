package com.softartdev.poder.ui.media

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.softartdev.poder.R
import com.softartdev.poder.media.MediaPlaybackService
import com.softartdev.poder.media.MediaProvider
import com.softartdev.poder.media.MediaUtils
import com.softartdev.poder.ui.base.BaseActivity
import kotlinx.android.synthetic.main.activity_media_playback.*
import timber.log.Timber
import javax.inject.Inject

class MediaPlaybackActivity(override val layout: Int = R.layout.activity_media_playback) : BaseActivity(), MediaPlaybackView {
    @Inject lateinit var mediaPlaybackPresenter: MediaPlaybackPresenter

    private var progressSeekBar: SeekBar? = null
    private var lastSeekEventTime: Long = 0

    private var mediaBrowserCompat: MediaBrowserCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent().inject(this)
        mediaPlaybackPresenter.attachView(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        volumeControlStream = AudioManager.STREAM_MUSIC

        media_prev_button.setOnClickListener {
            MediaControllerCompat.getMediaController(this@MediaPlaybackActivity)?.let {
                if (it.playbackState.position < 2000) {
                    it.transportControls.skipToPrevious()
                } else {
                    it.transportControls.seekTo(0)
                    it.transportControls.play()
                }
            }
        }
        media_play_button.requestFocus()
        media_play_button.setOnClickListener {
            MediaControllerCompat.getMediaController(this@MediaPlaybackActivity)?.let {
                val playing = it.playbackState.state == PlaybackStateCompat.STATE_PLAYING
                it.transportControls.apply { if (playing) pause() else play() }
            }
        }
        media_next_button.setOnClickListener {
            MediaControllerCompat.getMediaController(this@MediaPlaybackActivity)?.transportControls?.skipToNext()
        }
        media_shuffle_button.setOnClickListener { Toast.makeText(this, "Shuffle not implemented yet", Toast.LENGTH_SHORT).show() }
        media_queue_button.setOnClickListener { Toast.makeText(this, "Queue not implemented yet", Toast.LENGTH_SHORT).show() }
        media_repeat_button.setOnClickListener {
            MediaControllerCompat.getMediaController(this@MediaPlaybackActivity)?.let {
                fun repeat(nextRepeatMode: MediaPlaybackService.RepeatMode, imageRes: Int) {
                    it.transportControls.sendCustomAction(MediaPlaybackService.CMD_REPEAT, Bundle().apply {
                        putInt(MediaPlaybackService.REPEAT_MODE, nextRepeatMode.ordinal)
                    })
                    media_repeat_button.setImageResource(imageRes)
                }
                when (MediaPlaybackService.RepeatMode.values()[it.extras.getInt(MediaPlaybackService.REPEAT_MODE)]) {
                    MediaPlaybackService.RepeatMode.REPEAT_NONE -> repeat(MediaPlaybackService.RepeatMode.REPEAT_ALL, R.drawable.ic_repeat_black_24dp)
                    MediaPlaybackService.RepeatMode.REPEAT_ALL -> repeat(MediaPlaybackService.RepeatMode.REPEAT_CURRENT, R.drawable.ic_repeat_one_black_24dp)
                    MediaPlaybackService.RepeatMode.REPEAT_CURRENT -> repeat(MediaPlaybackService.RepeatMode.REPEAT_NONE, R.drawable.ic_repeat_off_black_24dp)
                }
            }
        }
        progressSeekBar = findViewById(android.R.id.progress)
        if (progressSeekBar is SeekBar) {
            val seekBar: SeekBar = progressSeekBar as SeekBar
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                var mmFromTouch = false
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    lastSeekEventTime = 0
                    mmFromTouch = true
                }

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        MediaControllerCompat.getMediaController(this@MediaPlaybackActivity)?.let {
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastSeekEventTime > 250) {
                                lastSeekEventTime = now
                                val duration = it.metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                                val position = duration * progress / 1000
                                it.transportControls.seekTo(position)
                            }
                        }
                        // trackball event, allow progress updates
                        if (!mmFromTouch) {
                            updateProgressBar()
                        }
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    mmFromTouch = false
                }
            })
        } else {
            Timber.d("Seeking not supported")
        }
        progressSeekBar?.max = 1000

        val serviceComponent = ComponentName(this, MediaPlaybackService::class.java)
        mediaBrowserCompat = MediaBrowserCompat(this, serviceComponent, connectionCallBack, null)
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserCompat?.connect()
    }

    override fun onStop() {
        super.onStop()
        mediaBrowserCompat?.disconnect()
    }

    override fun onResume() {
        super.onResume()
        updateTrackInfo()
        setPauseButtonImage()
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state ?: return
            Timber.d("Received playback state change to state %s", state.toString())
            updateProgressBar()
            setPauseButtonImage()
        }
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata ?: return
            Timber.d("Received updated metadata: %s", metadata)
            updateTrackInfo()
        }
        override fun onSessionDestroyed() = Timber.d("Session destroyed. Need to fetch a new Media Session")
    }

    private val connectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            val sessionToken = mediaBrowserCompat?.sessionToken ?: throw IllegalArgumentException("No Session token")
            Timber.d("onConnected: session token %s", sessionToken)
            val mediaControllerCompat = MediaControllerCompat(this@MediaPlaybackActivity, sessionToken)
            mediaControllerCompat.registerCallback(mediaControllerCallback)
            MediaControllerCompat.setMediaController(this@MediaPlaybackActivity, mediaControllerCompat)
            setShuffleButtonImage()
            setPauseButtonImage()
            updateTrackInfo()
        }
        override fun onConnectionFailed() = Timber.d("onConnectionFailed")
        override fun onConnectionSuspended() = MediaControllerCompat.setMediaController(this@MediaPlaybackActivity, null)
    }

    private fun setShuffleButtonImage() {
        MediaControllerCompat.getMediaController(this@MediaPlaybackActivity) ?: return
        media_shuffle_button.setImageResource(R.drawable.ic_shuffle_black_24dp)
    }

    private fun setPauseButtonImage() {
        MediaControllerCompat.getMediaController(this@MediaPlaybackActivity)?.playbackState?.state?.let {
            media_play_button.setImageResource(if (it == PlaybackStateCompat.STATE_PLAYING) R.drawable.ic_pause_black_24dp else R.drawable.ic_play_arrow_black_24dp)
        }
    }

    private fun updateProgressBar(): Long {
        MediaControllerCompat.getMediaController(this@MediaPlaybackActivity)?.let {
            val duration = it.metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            val pos = it.playbackState.position
            if ((pos >= 0) && (duration > 0)) {
                media_current_time_text_view.text = MediaUtils.makeTimeString(this, pos / 1000)
                progressSeekBar?.progress = (1000 * pos / duration).toInt()

                if (it.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                    media_current_time_text_view.visibility = View.VISIBLE
                } else {
                    val vis = media_current_time_text_view.visibility
                    media_current_time_text_view.visibility = if (vis == View.INVISIBLE) View.VISIBLE else View.INVISIBLE
                    return 500
                }
            } else {
                media_current_time_text_view.text = "--:--"
                progressSeekBar?.progress = 1000
            }
            // calculate the number of milliseconds until the next full second, so the counter can be updated at just the right time
            val remaining = 1000 - pos % 1000

            // approximate how often we would need to refresh the slider to move it smoothly
            var width = progressSeekBar?.width ?: 0
            if (width == 0) width = 320
            val smoothRefreshTime = duration / width

            if (smoothRefreshTime > remaining) return remaining
            return if (smoothRefreshTime < 20) 20 else smoothRefreshTime
        } ?: return 500
    }

    private fun updateTrackInfo() {
        Timber.d("Update track info")
        MediaControllerCompat.getMediaController(this@MediaPlaybackActivity)?.metadata?.let {
            media_track_name_text_view.text = it.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            Timber.d("Track Name: %s", media_track_name_text_view.text)
            val artistName = it.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            media_artist_name_text_view.text = if (artistName == MediaProvider.UNKNOWN) getString(R.string.unknown_artist_name) else artistName
            val albumName = it.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            media_album_name_text_view.text = if (albumName == MediaProvider.UNKNOWN) getString(R.string.unknown_album_name) else albumName
            it.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)?.let { media_album_image_view.setImageBitmap(it) }
            val duration = it.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            media_total_time_text_view.text = MediaUtils.makeTimeString(this, duration / 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlaybackPresenter.detachView()
    }
}
