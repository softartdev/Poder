package com.softartdev.poder.ui.main.podcasts

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.softartdev.poder.R
import com.softartdev.poder.media.MediaIDHelper
import com.softartdev.poder.media.MediaPlaybackService
import com.softartdev.poder.ui.base.BaseFragment
import com.softartdev.poder.ui.common.ErrorView
import kotlinx.android.synthetic.main.fragment_podcasts.*
import kotlinx.android.synthetic.main.view_error.view.*
import timber.log.Timber
import javax.inject.Inject

class PodcastsFragment : BaseFragment(), PodcastsView, PodcastsAdapter.ClickListener, ErrorView.ErrorListener {
    @Inject lateinit var podcastsAdapter: PodcastsAdapter
    @Inject lateinit var podcastsPresenter: PodcastsPresenter

    private var mediaBrowserCompat: MediaBrowserCompat? = null

    override fun layoutId(): Int = R.layout.fragment_podcasts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentComponent().inject(this)
        podcastsPresenter.attachView(this)
        podcastsAdapter.clickListener = this

        val serviceComponent = ComponentName(context, MediaPlaybackService::class.java)
        mediaBrowserCompat = MediaBrowserCompat(context, serviceComponent, connectionCallBack, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        podcasts_swipe_refresh?.apply {
            setProgressBackgroundColorSchemeResource(R.color.primary)
            setColorSchemeResources(R.color.white)
            setOnRefreshListener { podcastsPresenter.refreshPodcasts() }
        }

        podcasts_recycler_view?.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(podcasts_recycler_view.context, DividerItemDecoration.VERTICAL))
            adapter = podcastsAdapter
        }

        podcasts_error_view?.setErrorListener(this)
/*
        if (podcastsAdapter.itemCount == 0) {
            podcastsPresenter.podcasts()
        }
*/
    }

    override fun onStart() {
        super.onStart()
        mediaBrowserCompat?.connect()
    }

    override fun onStop() {
        super.onStop()
        mediaBrowserCompat?.disconnect()
    }

    private val connectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Timber.d("onConnected")
            mediaBrowserCompat?.subscribe(MediaIDHelper.MEDIA_ID_MUSICS_BY_SONG, subscriptionCallback)
            activity?.let { MediaControllerCompat.getMediaController(it)?.registerCallback(mediaControllerCallback) }
        }
        override fun onConnectionSuspended() {
            Timber.d("onConnectionFailed")
        }
        override fun onConnectionFailed() {
            Timber.d("onConnectionSuspended")
        }
    }

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            showPodcasts(children)
        }
        override fun onError(parentId: String) {
            val errorMessage = getString(R.string.error_loading_media)
            showError(errorMessage)
            Timber.d("$errorMessage with parentId: $parentId")
        }
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let {
                podcastsAdapter.playbackState = it.state
                podcastsAdapter.notifyDataSetChanged()
            }
        }
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.let {
                podcastsAdapter.playbackMediaId = it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                podcastsAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun showPodcasts(podcasts: List<MediaBrowserCompat.MediaItem>) {
        podcastsAdapter.apply {
            mediaList = podcasts
            notifyDataSetChanged()
        }
    }

    override fun onMediaIdClick(mediaId: String) {
        podcastsPresenter.play(mediaId)
        activity?.let { MediaControllerCompat.getMediaController(it)?.transportControls?.playFromMediaId(mediaId, null) }
    }

    override fun showProgress(show: Boolean) {
        if (podcasts_swipe_refresh.isRefreshing) {
            podcasts_swipe_refresh.isRefreshing = show
        } else {
            podcasts_progress_view.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun showError(throwable: Throwable) {
        podcasts_error_view?.visibility = View.VISIBLE
    }

    fun showError(errorMessage: String) {
        podcasts_error_view.visibility = View.VISIBLE
        podcasts_error_view.text_error_message.text = errorMessage
    }

    override fun onReloadData() {
        podcasts_error_view?.visibility = View.GONE
        podcastsPresenter.refreshPodcasts()
    }

    override fun onDestroy() {
        super.onDestroy()
        podcastsPresenter.detachView()
    }
}
