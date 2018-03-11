package com.softartdev.poder.ui.main.podcasts

import android.support.v4.media.MediaBrowserCompat
import com.softartdev.poder.ui.base.MvpView

interface PodcastsView : MvpView {
    fun showProgress(show: Boolean)
    fun showError(throwable: Throwable)
    fun showPodcasts(podcasts: List<MediaBrowserCompat.MediaItem>)
}
