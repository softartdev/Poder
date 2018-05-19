package com.softartdev.poder.ui.main.podcasts

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.softartdev.poder.R
import com.softartdev.poder.ui.base.BaseFragment
import com.softartdev.poder.ui.common.ErrorView
import kotlinx.android.synthetic.main.fragment_podcasts.*
import javax.inject.Inject

class PodcastsFragment : BaseFragment(), PodcastsView, PodcastsAdapter.ClickListener, ErrorView.ErrorListener {
    @Inject lateinit var podcastsAdapter: PodcastsAdapter
    @Inject lateinit var podcastsPresenter: PodcastsPresenter

    override fun layoutId(): Int = R.layout.fragment_podcasts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentComponent().inject(this)
        podcastsPresenter.attachView(this)
        podcastsAdapter.clickListener = this
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

        if (podcastsAdapter.itemCount == 0) {
            podcastsPresenter.podcasts()
        }
    }

    override fun showProgress(show: Boolean) {
        if (podcasts_swipe_refresh.isRefreshing) {
            podcasts_swipe_refresh.isRefreshing = show
        } else {
            podcasts_progress_view.visibility = if (show) View.VISIBLE else View.GONE
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

    override fun showError(throwable: Throwable) {
        podcasts_error_view?.visibility = View.VISIBLE
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
