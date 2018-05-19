package com.softartdev.poder.ui.main.podcasts

import com.softartdev.poder.data.DataManager
import com.softartdev.poder.injection.ConfigPersistent
import com.softartdev.poder.ui.base.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@ConfigPersistent
class PodcastsPresenter @Inject
constructor(private val dataManager: DataManager) : BasePresenter<PodcastsView>() {

    fun podcasts() {
        checkViewAttached()
        mvpView?.showProgress(true)
        dataManager.getPodcasts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ podcasts ->
                    mvpView?.showProgress(false)
                    mvpView?.showPodcasts(podcasts)
                }) { throwable ->
                    throwable.printStackTrace()
                    mvpView?.showProgress(false)
                    mvpView?.showError(throwable)
                }
    }

    fun play(mediaId: String) {
        Timber.d("Play media with id = $mediaId")
    }

}