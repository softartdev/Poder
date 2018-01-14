package com.softartdev.poder.ui.main.downloads

import android.support.annotation.DrawableRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.softartdev.poder.R
import com.softartdev.poder.injection.ConfigPersistent
import kotlinx.android.synthetic.main.item_download.view.*
import java.io.File
import javax.inject.Inject

@ConfigPersistent
class DownloadsAdapter @Inject
constructor() : RecyclerView.Adapter<DownloadsAdapter.DownloadsViewHolder>() {
    private var fileList: List<File>
    private var clickListener: ClickListener? = null

    init {
        fileList = emptyList()
    }

    fun setFiles(files: List<File>) {
        this.fileList = files
    }

    fun setClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return DownloadsViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadsViewHolder, position: Int) {
        val file = fileList[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int = fileList.size

    interface ClickListener {
        fun onDownloadItemClick(file: File)
    }

    inner class DownloadsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private lateinit var selectedFile: File

        init {
            itemView.setOnClickListener { clickListener?.onDownloadItemClick(selectedFile) }
        }

        fun bind(file: File) {
            selectedFile = file
            @DrawableRes val drawableLeft = if (selectedFile.isDirectory) R.drawable.ic_folder_black_24dp else R.drawable.ic_insert_drive_file_black_24dp
            itemView.item_download_file_name_text_view?.apply {
                text = selectedFile.name
                setCompoundDrawablesWithIntrinsicBounds(drawableLeft, 0, 0, 0)
            }
        }
    }

}
