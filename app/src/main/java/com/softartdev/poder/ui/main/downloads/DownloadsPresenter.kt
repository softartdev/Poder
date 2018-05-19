package com.softartdev.poder.ui.main.downloads

import com.softartdev.poder.data.DataManager
import com.softartdev.poder.injection.ConfigPersistent
import com.softartdev.poder.ui.base.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ConfigPersistent
class DownloadsPresenter @Inject
constructor(private val dataManager: DataManager) : BasePresenter<DownloadsView>() {

    fun downloads() {
        checkViewAttached()
        mvpView?.showProgress(true)
        addDisposable(dataManager.getDownloads()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ files ->
                    mvpView?.showProgress(false)
                    mvpView?.showFiles(files.toList())
                }) { throwable ->
                    throwable.printStackTrace()
                    mvpView?.showProgress(false)
                    mvpView?.showError(throwable)
                })
    }

}
