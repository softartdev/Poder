package com.softartdev.poder.ui.main.downloads

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.FileProvider
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View

import com.softartdev.poder.R
import com.softartdev.poder.ui.base.BaseFragment
import com.softartdev.poder.ui.common.ErrorView
import com.softartdev.poder.util.visible
import kotlinx.android.synthetic.main.fragment_downloads.*
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import android.webkit.MimeTypeMap



class DownloadsFragment : BaseFragment(), DownloadsView, DownloadsAdapter.ClickListener, ErrorView.ErrorListener {

    @Inject lateinit var downloadsAdapter: DownloadsAdapter
    @Inject lateinit var downloadsPresenter: DownloadsPresenter

    override fun layoutId(): Int = R.layout.fragment_downloads

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentComponent().inject(this)
        downloadsPresenter.attachView(this)
        downloadsAdapter.setClickListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        downloads_swipe_refresh?.apply {
            setProgressBackgroundColorSchemeResource(R.color.primary)
            setColorSchemeResources(R.color.white)
            setOnRefreshListener { downloadsPresenter.downloads() }
        }

        downloadsAdapter.setClickListener(this)
        downloads_recycler_view?.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(downloads_recycler_view.context, DividerItemDecoration.VERTICAL))
            adapter = downloadsAdapter
        }

        downloads_error_view?.setErrorListener(this)

        if (downloadsAdapter.itemCount == 0) {
            downloadsPresenter.downloads()
        }
    }

    override fun showFiles(files: List<File>) {
        downloadsAdapter.apply {
            setFiles(files)
            notifyDataSetChanged()
        }
    }

    override fun onDownloadItemClick(file: File) {
        val downloadsPath = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS)
        val downloadFile = File(downloadsPath, file.name)
        val contentUri = context?.let { FileProvider.getUriForFile(it, "com.softartdev.poder.fileprovider", downloadFile) }

        val map = MimeTypeMap.getSingleton()
        val ext = MimeTypeMap.getFileExtensionFromUrl(downloadFile.name)
        val type = map.getMimeTypeFromExtension(ext) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(contentUri, type)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    override fun showProgress(show: Boolean) {
        if (downloads_swipe_refresh.isRefreshing) {
            downloads_swipe_refresh.isRefreshing = show
        } else {
            downloads_progress_view.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun showError(throwable: Throwable) {
        downloads_error_view?.visible()
        Timber.e(throwable, "There was an error retrieving the download")
    }

    override fun onReloadData() {
        downloadsPresenter.downloads()
    }
}
