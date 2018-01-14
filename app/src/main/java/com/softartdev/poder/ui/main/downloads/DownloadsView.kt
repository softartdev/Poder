package com.softartdev.poder.ui.main.downloads

import com.softartdev.poder.ui.base.MvpView
import java.io.File

interface DownloadsView : MvpView {
    fun showProgress(show: Boolean)
    fun showFiles(files: List<File>)
    fun showError(throwable: Throwable)
}
