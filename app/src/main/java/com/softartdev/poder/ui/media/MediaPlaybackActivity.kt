package com.softartdev.poder.ui.media

import android.os.Bundle
import com.softartdev.poder.R
import com.softartdev.poder.ui.base.BaseActivity
import javax.inject.Inject

class MediaPlaybackActivity(override val layout: Int = R.layout.activity_media_playback) : BaseActivity(), MediaPlaybackView {

    @Inject lateinit var mediaPlaybackPresenter: MediaPlaybackPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent().inject(this)
        mediaPlaybackPresenter.attachView(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlaybackPresenter.detachView()
    }
}
