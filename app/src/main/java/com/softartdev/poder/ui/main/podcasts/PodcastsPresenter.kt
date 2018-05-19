package com.softartdev.poder.ui.main.podcasts

import android.support.v4.media.MediaBrowserCompat
import com.softartdev.poder.data.DataManager
import com.softartdev.poder.injection.ConfigPersistent
import com.softartdev.poder.ui.base.BasePresenter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@ConfigPersistent
class PodcastsPresenter @Inject
constructor(private val dataManager: DataManager) : BasePresenter<PodcastsView>() {

    fun podcasts() = addSubscription(dataManager.getPodcasts())

    fun refreshPodcasts() = addSubscription(dataManager.updatePodcasts())

    private fun addSubscription(singlePodcasts: Single<List<MediaBrowserCompat.MediaItem>>) {
        checkViewAttached()
        mvpView?.showProgress(true)
        addDisposable(singlePodcasts
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ podcasts ->
                    mvpView?.showProgress(false)
                    mvpView?.showPodcasts(podcasts)
                }) { throwable ->
                    throwable.printStackTrace()
                    mvpView?.showProgress(false)
                    mvpView?.showError(throwable)
                })
    }

    fun play(mediaId: String) {
        Timber.d("Play media with id = $mediaId")
    }

}