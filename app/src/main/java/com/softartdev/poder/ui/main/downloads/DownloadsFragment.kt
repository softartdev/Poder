package com.softartdev.poder.ui.main.downloads

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.webkit.MimeTypeMap
import com.softartdev.poder.R
import com.softartdev.poder.ui.base.BaseFragment
import com.softartdev.poder.ui.common.ErrorView
import com.softartdev.poder.util.gone
import com.softartdev.poder.util.visible
import com.tbruyelle.rxpermissions.RxPermissions
import kotlinx.android.synthetic.main.fragment_downloads.*
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class DownloadsFragment : BaseFragment(), DownloadsView, DownloadsAdapter.ClickListener, ErrorView.ErrorListener, DialogInterface.OnClickListener {

    @Inject lateinit var downloadsAdapter: DownloadsAdapter
    @Inject lateinit var downloadsPresenter: DownloadsPresenter

    private var rxPermissions: RxPermissions? = null

    override fun layoutId(): Int = R.layout.fragment_downloads

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentComponent().inject(this)
        downloadsPresenter.attachView(this)
        downloadsAdapter.setClickListener(this)
        rxPermissions = RxPermissions(activity as Activity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        downloads_swipe_refresh?.apply {
            setProgressBackgroundColorSchemeResource(R.color.primary)
            setColorSchemeResources(R.color.white)
            setOnRefreshListener { showDownloadsIfPermissionGranted() }
        }

        downloads_recycler_view?.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(downloads_recycler_view.context, DividerItemDecoration.VERTICAL))
            adapter = downloadsAdapter
        }

        downloads_error_view?.setErrorListener(this)

        if (downloadsAdapter.itemCount == 0) {
            showDownloadsIfPermissionGranted()
        }
    }

    private fun showDownloadsIfPermissionGranted() {
        if (rxPermissions!!.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            downloadsPresenter.downloads()
        } else {
            rxPermissions!!.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe { granted ->
                        if (granted) {
                            showDownloadsIfPermissionGranted()
                        } else {
                            showRepeatableErrorWithSettings()
                        }
                    }
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

        val ext = MimeTypeMap.getFileExtensionFromUrl(downloadFile.name)
        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

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
        downloads_error_view?.gone()
        showDownloadsIfPermissionGranted()
    }

    private fun showRepeatableErrorWithSettings() {
        context?.let {
            AlertDialog.Builder(it)
                    .setMessage(R.string.rationale_storage_permission)
                    .setNegativeButton(R.string.dialog_action_cancel, this)
                    .setPositiveButton(R.string.retry, this)
                    .setNeutralButton(R.string.settings, this)
                    .setCancelable(false)
                    .show()
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_NEGATIVE -> {
                dialog.cancel()
                showError(IllegalStateException("No permission"))
            }
            DialogInterface.BUTTON_POSITIVE -> {
                dialog.cancel()
                showDownloadsIfPermissionGranted()
            }
            DialogInterface.BUTTON_NEUTRAL -> {
                dialog.cancel()
                val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context?.packageName))
                startActivityForResult(appSettingsIntent, REQUEST_PERMISSION_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PERMISSION_EXTERNAL_STORAGE -> showDownloadsIfPermissionGranted()
        }
    }

    companion object {
        internal const val REQUEST_PERMISSION_EXTERNAL_STORAGE = 1004
    }
}
