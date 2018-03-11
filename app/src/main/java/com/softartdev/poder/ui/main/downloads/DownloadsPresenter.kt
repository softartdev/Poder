package com.softartdev.poder.ui.main.downloads

import com.softartdev.poder.data.DataManager
import com.softartdev.poder.injection.ConfigPersistent
import com.softartdev.poder.ui.base.BasePresenter
import com.softartdev.poder.util.rx.scheduler.SchedulerUtils
import java.io.File
import javax.inject.Inject

@ConfigPersistent
class DownloadsPresenter @Inject
constructor(private val dataManager: DataManager) : BasePresenter<DownloadsView>() {

    fun downloads() {
        checkViewAttached()
        mvpView?.showProgress(true)
        dataManager.getDownloads()
                .compose(SchedulerUtils.ioToMain<Array<File>>())
                .subscribe({ files ->
                    mvpView?.showProgress(false)
                    mvpView?.showFiles(files.toList())
                }) { throwable ->
                    throwable.printStackTrace()
                    mvpView?.showProgress(false)
                    mvpView?.showError(throwable)
                }
    }

}
