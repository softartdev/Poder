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

    enum class RepeatMode {
        REPEAT_NONE, REPEAT_ALL, REPEAT_CURRENT
    }

    companion object {
        private const val STOP_DELAY = 30000 // Delay stopSelf by using a handler.
        const val ACTION_CMD = "com.android.music.ACTION_CMD"
        const val CMD_NAME = "CMD_NAME"
        const val CMD_PAUSE = "CMD_PAUSE"
        const val CMD_REPEAT = "CMD_PAUSE"
        const val REPEAT_MODE = "REPEAT_MODE"
    }

}
