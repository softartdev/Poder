package com.softartdev.poder.media

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.softartdev.poder.PoderApp
import com.softartdev.poder.R
import com.softartdev.poder.ui.main.MainActivity
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.ArrayList
import javax.inject.Inject

class MediaPlaybackService : MediaBrowserServiceCompat(), Playback.Callback {
    @Inject lateinit var mediaProvider: MediaProvider

    private var mSession: MediaSessionCompat? = null
    // "Now playing" queue:
    private var mPlayingQueue: List<MediaSessionCompat.QueueItem> = emptyList()
    private var mCurrentIndexOnQueue = -1
    private var mMediaNotificationManager: MediaNotificationManager? = null
    // Indicates whether the service was started.
    private var mServiceStarted: Boolean = false
    private var mDelayedStopHandler: DelayedStopHandler? = null
    private var mPlayback: Playback? = null
    // Default mode is repeat none
    private var mRepeatMode = RepeatMode.REPEAT_NONE
    // Extra information for this session
    private var mExtras: Bundle? = null

    override fun onCreate() {
        super.onCreate()
        PoderApp[this].component.inject(this)

        Timber.d("Create MediaSessionCompat")
        // Start a new MediaSessionCompat
        mSession = MediaSessionCompat(this, "MediaPlaybackService")
        // Set extra information
        mExtras = Bundle()
        mExtras?.putInt(REPEAT_MODE, mRepeatMode.ordinal)
        mSession?.setExtras(mExtras)
        // Enable callbacks from MediaButtons and TransportControls
        mSession?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        // Set an initial PlaybackStateCompat with ACTION_PLAY, so media buttons can start the player
        val stateBuilder = PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
        mSession?.setPlaybackState(stateBuilder.build())
        // MediaSessionCompatCallback() has methods that handle callbacks from a media controller
        mSession?.setCallback(MediaSessionCompatCallback())
        // Set the session's token so that client activities can communicate with it.
        sessionToken = mSession?.sessionToken

        mDelayedStopHandler = DelayedStopHandler(this)

        mPlayback = Playback(this, mediaProvider)
        mPlayback?.state = PlaybackStateCompat.STATE_NONE
        mPlayback?.callback = this

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mSession?.setSessionActivity(pendingIntent)

        updatePlaybackStateCompat(null)

        mMediaNotificationManager = MediaNotificationManager(this)
    }

    override fun onStartCommand(startIntent: Intent?, flags: Int, startId: Int): Int {
        if (ACTION_CMD == startIntent?.action
                && CMD_PAUSE == startIntent.getStringExtra(CMD_NAME)
                && mPlayback?.isPlaying == true) {
            handlePauseRequest()
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        // Service is being killed, so make sure we release our resources
        handleStopRequest(null)

        mDelayedStopHandler?.removeCallbacksAndMessages(null)
        // Always release the MediaSessionCompat to clean up resources
        // and notify associated MediaController(s).
        mSession?.release()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
        Timber.d("OnGetRoot: clientPackageName=%s; clientUid=%s ; rootHints=%s", clientPackageName, clientUid, rootHints)
        // Allow everyone to browse
        return MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_ROOT, null)
    }

    override fun onLoadChildren(parentMediaId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
        Timber.d("OnLoadChildren: parentMediaId=%s", parentMediaId)
        if (mediaProvider.isInitialized) {
            // If our music catalog is already loaded/cached, load them into result immediately
            val mediaItems = ArrayList<MediaBrowserCompat.MediaItem>()

            when (parentMediaId) {
                MEDIA_ID_ROOT -> {
                    Timber.d("OnLoadChildren.ROOT")
                    val podcastsTitle = getString(R.string.title_podcasts)
                    val podcastsDescription = with(MediaDescriptionCompat.Builder()) {
                        setMediaId(MEDIA_ID_PODCAST)
                        setTitle(podcastsTitle)
                        build()
                    }
                    val podcastsItem = MediaBrowserCompat.MediaItem(podcastsDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
                    mediaItems.add(podcastsItem)
                }
                MEDIA_ID_PODCAST -> {
                    Timber.d("onLoadChildren.PODCAST")
                    loadPodcast(mediaProvider.metadataList, mediaItems)
                }
                else -> {
                    Timber.w("Skipping unmatched parentMediaId: %s", parentMediaId)
                }
            }
            Timber.d("OnLoadChildren sending %s results for %s", mediaItems.size, parentMediaId)
            result.sendResult(mediaItems)
        } else {
            // Use result.detach to allow calling result.sendResult from another thread:
            result.detach()
            mediaProvider.podcasts = null
            val podcasts = mediaProvider.podcasts ?: emptyList()
            Timber.d("Retrieved %s items", podcasts.size)
            onLoadChildren(parentMediaId, result)
        }
    }

    private fun loadPodcast(songList: Iterable<MediaMetadataCompat>, mediaItems: MutableList<MediaBrowserCompat.MediaItem>) {
        for (metadata in songList) {
            val hierarchyAwareMediaID = MEDIA_ID_ROOT + CATEGORY_SEPARATOR + MEDIA_ID_PODCAST + LEAF_SEPARATOR + metadata.description.mediaId
            val songExtra = Bundle()
            songExtra.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
            val title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            val artistName = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            val description: MediaDescriptionCompat = with(MediaDescriptionCompat.Builder()) {
                setMediaId(hierarchyAwareMediaID)
                setTitle(title)
                setSubtitle(artistName)
                setIconBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                setExtras(songExtra)
                build()
            }
            val item = MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
            mediaItems.add(item)
        }
    }

    private inner class MediaSessionCompatCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Timber.d("play")
            if (mPlayingQueue.isNotEmpty()) {
                handlePlayRequest()
            }
        }

        override fun onSkipToQueueItem(queueId: Long) {
            Timber.d("OnSkipToQueueItem:%s", queueId)
            if (mPlayingQueue.isNotEmpty()) {
                // set the current index on queue from the music Id:
                mCurrentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId)
                // play the music
                handlePlayRequest()
            }
        }

        override fun onSeekTo(position: Long) {
            Timber.d("onSeekTo:%s", position)
            mPlayback?.seekTo(position.toInt())
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            Timber.d("playFromMediaId mediaId:%s extras=%s", mediaId, extras)
            // The mediaId used here is not the unique musicId. This one comes from the
            // MediaBrowser, and is actually a "hierarchy-aware mediaID": a concatenation of
            // the hierarchy in MediaBrowser and the actual unique musicID. This is necessary
            // so we can build the correct playing queue, based on where the track was
            // selected from.
            QueueHelper.getPlayingQueue(mediaId, mediaProvider)?.let { mPlayingQueue = it }
            mSession?.setQueue(mPlayingQueue)
            val queueTitle = getString(R.string.browse_podcasts_subtitle, mPlayingQueue.size.toString())
            mSession?.setQueueTitle(queueTitle)
            if (mPlayingQueue.isNotEmpty()) {
                // set the current index on queue from the media Id:
                mCurrentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, mediaId)
                if (mCurrentIndexOnQueue < 0) {
                    Timber.e("playFromMediaId: media ID %s could not be found on queue. Ignoring.", mediaId)
                } else {
                    // play the music
                    handlePlayRequest()
                }
            }
        }

        override fun onPause() {
            Timber.d("pause. current state=%s", mPlayback?.state)
            handlePauseRequest()
        }

        override fun onStop() {
            Timber.d("stop. current state=%s", mPlayback?.state)
            handleStopRequest(null)
        }

        override fun onSkipToNext() {
            Timber.d("skipToNext")
            mCurrentIndexOnQueue++
            if (mCurrentIndexOnQueue >= mPlayingQueue.size) {
                // This sample's behavior: skipping to next when in last song returns to the
                // first song.
                mCurrentIndexOnQueue = 0
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                handlePlayRequest()
            } else {
                Timber.e("skipToNext: cannot skip to next. next Index=$mCurrentIndexOnQueue queue length=${mPlayingQueue.size}")
                handleStopRequest("Cannot skip")
            }
        }

        override fun onSkipToPrevious() {
            Timber.d("skipToPrevious")
            mCurrentIndexOnQueue--
            if (mCurrentIndexOnQueue < 0) {
                // This sample's behavior: skipping to previous when in first song restarts the
                // first song.
                mCurrentIndexOnQueue = 0
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                handlePlayRequest()
            } else {
                Timber.e("skipToPrevious: cannot skip to previous. previous Index=$mCurrentIndexOnQueue queue length=${mPlayingQueue.size}")
                handleStopRequest("Cannot skip")
            }
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) = Timber.d("playFromSearch  query=%s", query)

        override fun onCustomAction(action: String?, extras: Bundle?) {
            Timber.d("onCustomAction action=%s, extras=%s", action, extras)
            when (action) {
                CMD_REPEAT -> {
                    extras?.getInt(REPEAT_MODE)?.let { mRepeatMode = RepeatMode.values()[it] }
                    mExtras?.putInt(REPEAT_MODE, mRepeatMode.ordinal)
                    mSession?.setExtras(mExtras)
                    Timber.d("modified repeatMode=%s", mRepeatMode)
                }
                else -> Timber.d("Unknown action=%s", action)
            }
        }
    }

    /**
     * Handle a request to play music
     */
    private fun handlePlayRequest() {
        Timber.d("handlePlayRequest: mState=%s", mPlayback?.state)
        mDelayedStopHandler?.removeCallbacksAndMessages(null)
        if (!mServiceStarted) {
            Timber.v("Starting service")
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(Intent(applicationContext, MediaPlaybackService::class.java))
            mServiceStarted = true
        }

        if (mSession?.isActive == false) {
            mSession?.isActive = true
        }

        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            updateMetadata()
            mPlayback?.play(mPlayingQueue[mCurrentIndexOnQueue])
        }
    }

    /**
     * Handle a request to pause music
     */
    private fun handlePauseRequest() {
        Timber.d("handlePauseRequest: mState=%s", mPlayback?.state)
        mPlayback?.pause()
        // reset the delayed stop handler.
        mDelayedStopHandler?.removeCallbacksAndMessages(null)
        mDelayedStopHandler?.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())
    }

    /**
     * Handle a request to stop music
     */
    private fun handleStopRequest(withError: String?) {
        Timber.d("handleStopRequest: mState=%s error=%s", mPlayback?.state, withError)
        mPlayback?.stop(true)
        // reset the delayed stop handler.
        mDelayedStopHandler?.removeCallbacksAndMessages(null)
        mDelayedStopHandler?.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())

        updatePlaybackStateCompat(withError)

        // service is no longer necessary. Will be started again if needed.
        stopSelf()
        mServiceStarted = false
    }

    private fun updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            Timber.e("Can't retrieve current metadata.")
            updatePlaybackStateCompat(resources.getString(R.string.error_no_metadata))
            return
        }
        val queueItem = mPlayingQueue[mCurrentIndexOnQueue]
        val podcastId = queueItem.description.mediaId?.let { MediaUtils.removeMediaIdPrefix(it) }
        val track = mediaProvider.getMediaById(podcastId)?.metadata
        val trackId = track?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        if (podcastId != trackId) {
            val e = IllegalStateException("track ID should match podcastId.")
            val errorMessage = "track ID should match musicId. podcastId=$podcastId trackId=$trackId \n " +
                    "mediaId from queueItem=${queueItem.description.mediaId} title from queueItem=${queueItem.description.title} \n " +
                    "mediaId from track=${track?.description?.mediaId} title from track=${track?.description?.title} source.hashcode from track=${track?.getString(MediaProvider.CUSTOM_METADATA_TRACK_SOURCE)?.hashCode()}"
            Timber.e(e, errorMessage)
            throw e
        }
        Timber.d("Updating metadata for PodcastID=%s", podcastId)
        mSession?.setMetadata(track)

        // Set the proper album artwork on the media session, so it can be shown in the
        // locked screen and in other places.
        if (track?.description?.iconBitmap == null && track?.description?.iconUri != null) {
            val albumUri = track.description.iconUri.toString()
            AlbumArtCache.fetch(albumUri, object : AlbumArtCache.FetchListener() {
                override fun onFetched(artUrl: String, bigImage: Bitmap, iconImage: Bitmap) {
                    var trackMetadata = mediaProvider.getMediaById(trackId)?.metadata
                    trackMetadata = MediaMetadataCompat.Builder(trackMetadata)
                            // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                            // example, on the lock screen background when the media session is active.
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bigImage)
                            // set small version of the album art in the DISPLAY_ICON. This is used on
                            // the MediaDescriptionCompat and thus it should be small to be serialized if necessary..
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, iconImage)
                            .build()
                    // If we are still playing the same music
                    val currentPlayingId = queueItem.description.mediaId
                    if (trackId == currentPlayingId) {
                        mSession?.setMetadata(trackMetadata)
                    }
                }
            })
        }
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    private fun updatePlaybackStateCompat(error: String?) {
        Timber.d("updatePlaybackStateCompat, playback state=%s", mPlayback?.state)
        val position: Long = mPlayback?.currentStreamPosition
                ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
        var state: Int = mPlayback?.state ?: 0

        val stateBuilder = PlaybackStateCompat.Builder().setActions(availableActions())
        // If there is an error message, send it to the playback state:
        error?.let {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(0, it)
            state = PlaybackStateCompat.STATE_ERROR
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())

        // Set the activeQueueItemId if the current index is valid.
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            val item = mPlayingQueue[mCurrentIndexOnQueue]
            stateBuilder.setActiveQueueItemId(item.queueId)
        }
        mSession?.setPlaybackState(stateBuilder.build())
        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            mMediaNotificationManager?.startNotification()
        }
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    override fun onCompletion() {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mPlayingQueue.isEmpty()) {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null)
        } else {
            when (mRepeatMode) {
                MediaPlaybackService.RepeatMode.REPEAT_ALL -> {
                    // Increase the index
                    mCurrentIndexOnQueue++
                    // Restart queue when reaching the end
                    if (mCurrentIndexOnQueue >= mPlayingQueue.size) {
                        mCurrentIndexOnQueue = 0
                    }
                }
                MediaPlaybackService.RepeatMode.REPEAT_CURRENT -> {
                }
                MediaPlaybackService.RepeatMode.REPEAT_NONE -> {
                    // Increase the index
                    mCurrentIndexOnQueue++
                    // Stop the queue when reaching the end
                    if (mCurrentIndexOnQueue >= mPlayingQueue.size) {
                        handleStopRequest(null)
                        return
                    }
                }
            }// Do not change the index
            handlePlayRequest()
        }
    }

    override fun onPlaybackStatusChanged(state: Int) {
        updatePlaybackStateCompat(null)
    }

    override fun onError(error: String?) {
        updatePlaybackStateCompat(error)
    }

    private fun availableActions(): Long {
        var actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
        if (mPlayingQueue.isEmpty()) {
            return actions
        }
        if (mPlayback?.isPlaying == true) {
            actions = actions or PlaybackStateCompat.ACTION_PAUSE
        }
        if (mCurrentIndexOnQueue > 0) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size - 1) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        }
        return actions
    }

    enum class RepeatMode {
        REPEAT_NONE, REPEAT_ALL, REPEAT_CURRENT
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private class DelayedStopHandler constructor(service: MediaPlaybackService) : Handler() {
        private val mWeakReference: WeakReference<MediaPlaybackService> = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()
            if (service?.mPlayback?.isPlaying == true) {
                Timber.d("Ignoring delayed stop since the media player is in use.")
                return
            }
            Timber.d("Stopping service with delay handler.")
            service?.stopSelf()
            service?.mServiceStarted = false
        }
    }

    companion object {
        const val MEDIA_ID_ROOT = "__ROOT__"
        const val MEDIA_ID_PODCAST = "__PODCAST__"
        const val CATEGORY_SEPARATOR: Char = 31.toChar()
        const val LEAF_SEPARATOR: Char = 30.toChar()
        private const val STOP_DELAY = 30000 // Delay stopSelf by using a handler.
        const val ACTION_CMD = "com.android.music.ACTION_CMD"
        const val CMD_NAME = "CMD_NAME"
        const val CMD_PAUSE = "CMD_PAUSE"
        const val CMD_REPEAT = "CMD_PAUSE"
        const val REPEAT_MODE = "REPEAT_MODE"
    }

}
