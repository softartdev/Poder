package com.softartdev.poder.ui.main.podcasts

import com.softartdev.poder.injection.ConfigPersistent
import com.softartdev.poder.ui.base.BasePresenter
import timber.log.Timber
import javax.inject.Inject

@ConfigPersistent
class PodcastsPresenter @Inject
constructor(/*private val dataManager: DataManager*/) : BasePresenter<PodcastsView>() {

    fun podcasts() {
        checkViewAttached()
        mvpView?.showProgress(true)
        Timber.d("SHOWING PODCASTS")
    }

    fun play(mediaId: String) {
        Timber.d("Play media with id = $mediaId")
    }

}