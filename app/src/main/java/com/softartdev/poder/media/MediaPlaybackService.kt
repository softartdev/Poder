package com.softartdev.poder.media

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import com.softartdev.poder.PoderApp
import javax.inject.Inject

class MediaPlaybackService : MediaBrowserServiceCompat() {

    @Inject lateinit var mediaProvider: MediaProvider

    override fun onCreate() {
        super.onCreate()
        PoderApp[this].component.inject(this)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        //TODO
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        //TODO
        return null
    }

}
