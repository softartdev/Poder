package com.softartdev.poder.ui.main.podcasts

import com.softartdev.poder.ui.base.MvpView

interface PodcastsView : MvpView {
    fun showProgress(show: Boolean)
    fun showError(throwable: Throwable)
}
