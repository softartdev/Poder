package com.softartdev.poder.ui.main

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.softartdev.poder.R
import com.softartdev.poder.media.MediaPlaybackService
import com.softartdev.poder.media.MediaProvider
import com.softartdev.poder.ui.media.MediaPlaybackActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.playback_controls.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var mediaBrowserCompat: MediaBrowserCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(main_toolbar)

        val serviceComponent = ComponentName(this, MediaPlaybackService::class.java)
        mediaBrowserCompat = MediaBrowserCompat(this, serviceComponent, connectionCallBack, null)

        main_playback_card_view.setOnClickListener { startActivity(Intent(this, MediaPlaybackActivity::class.java)) }
        play_pause_image_button.setOnClickListener {
            MediaControllerCompat.getMediaController(this@MainActivity)?.let {
                val playing = it.playbackState.state == PlaybackStateCompat.STATE_PLAYING
                it.transportControls.apply { if (playing) pause() else play() }
            }
        }

        main_bottom_navigation_view.setOnNavigationItemSelectedListener(MainNavigation(supportFragmentManager))
        if (savedInstanceState == null) {
            main_bottom_navigation_view.selectedItemId = R.id.navigation_downloads
        }
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
        setPauseButtonImage()
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) = setPauseButtonImage()
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) = updateNowPlaying(metadata)
        override fun onSessionDestroyed() = Timber.d("Session destroyed. Need to fetch a new Media Session")
    }

    private val connectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            val sessionToken = mediaBrowserCompat?.sessionToken ?: throw IllegalArgumentException("No Session token")
            Timber.d("onConnected: session token %s", sessionToken)
            val mediaControllerCompat = MediaControllerCompat(this@MainActivity, sessionToken)
            mediaControllerCompat.registerCallback(mediaControllerCallback)
            MediaControllerCompat.setMediaController(this@MainActivity, mediaControllerCompat)
            setPauseButtonImage()
            updateNowPlaying(mediaControllerCompat.metadata)
        }
        override fun onConnectionFailed() = Timber.d("onConnectionFailed")
        override fun onConnectionSuspended() = MediaControllerCompat.setMediaController(this@MainActivity, null)
    }

    private fun updateNowPlaying(metadata: MediaMetadataCompat?) {
        title_text_view.text = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        val artistName = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        subtitle_text_view.text = if (artistName == MediaProvider.UNKNOWN) getString(R.string.unknown_artist_name) else artistName
        metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)?.let { album_image_view.setImageBitmap(it) }
        main_playback_card_view.visibility = if (metadata != null) View.VISIBLE else View.GONE
    }

    private fun setPauseButtonImage() {
        MediaControllerCompat.getMediaController(this@MainActivity)?.playbackState?.state?.let {
            play_pause_image_button.setImageResource(if (it == PlaybackStateCompat.STATE_PLAYING) R.drawable.ic_pause_black_24dp else R.drawable.ic_play_arrow_black_24dp)
        }
    }
}
