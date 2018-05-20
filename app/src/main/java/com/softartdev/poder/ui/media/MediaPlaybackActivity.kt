package com.softartdev.poder.ui.media

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import com.softartdev.poder.R
import com.softartdev.poder.media.MediaPlaybackService
import com.softartdev.poder.ui.base.BaseActivity
import timber.log.Timber
import javax.inject.Inject

class MediaPlaybackActivity(override val layout: Int = R.layout.activity_media_playback) : BaseActivity(), MediaPlaybackView {

    @Inject lateinit var mediaPlaybackPresenter: MediaPlaybackPresenter

    private var mediaBrowserCompat: MediaBrowserCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent().inject(this)
        mediaPlaybackPresenter.attachView(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
//            MusicUtils.updateNowPlaying(this@MainActivity)
        }
    }

    private val connectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            val sessionToken = mediaBrowserCompat?.sessionToken ?: throw IllegalArgumentException("No Session token")
            Timber.d("onConnected: session token %s", sessionToken)
            val mediaControllerCompat = MediaControllerCompat(this@MediaPlaybackActivity, sessionToken)
            mediaControllerCompat.registerCallback(mediaControllerCallback)
            MediaControllerCompat.setMediaController(this@MediaPlaybackActivity, mediaControllerCompat)

            if (mediaControllerCompat.metadata != null) {
//                MusicUtils.updateNowPlaying(this@MainActivity)
            }
        }
        override fun onConnectionFailed() {
            Timber.d("onConnectionFailed")
        }
        override fun onConnectionSuspended() {
            Timber.d("onConnectionSuspended")
            MediaControllerCompat.setMediaController(this@MediaPlaybackActivity, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlaybackPresenter.detachView()
    }
}
