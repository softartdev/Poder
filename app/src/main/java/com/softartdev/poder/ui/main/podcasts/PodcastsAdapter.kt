package com.softartdev.poder.ui.main.podcasts

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.AnimationDrawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.softartdev.poder.R
import com.softartdev.poder.injection.ApplicationContext
import com.softartdev.poder.injection.ConfigPersistent
import kotlinx.android.synthetic.main.item_podcast.view.*
import javax.inject.Inject

@ConfigPersistent
class PodcastsAdapter @Inject
constructor(@ApplicationContext val context: Context) : RecyclerView.Adapter<PodcastsAdapter.MediaItemsViewHolder>() {
    var mediaList: List<MediaBrowserCompat.MediaItem> = emptyList()
    var clickListener: ClickListener? = null
    private val animation = ContextCompat.getDrawable(context, R.drawable.ic_equalizer_white_36dp) as AnimationDrawable
    private val colorStatePlaying = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent))
    var playbackMediaId: String = "METADATA_KEY_MEDIA_ID"
    var playbackState: Int = PlaybackStateCompat.STATE_NONE

    init {
        DrawableCompat.setTintList(animation, colorStatePlaying)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_podcast, parent, false)
        return MediaItemsViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaItemsViewHolder, position: Int) {
        val mediaDescriptionCompat = mediaList[position].description
        holder.bind(mediaDescriptionCompat)
    }

    override fun getItemCount(): Int = mediaList.size

    interface ClickListener {
        fun onMediaIdClick(mediaId: String)
    }

    inner class MediaItemsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(mediaDescriptionCompat: MediaDescriptionCompat) {
            itemView.item_podcast_title_text_view.text = mediaDescriptionCompat.title
            itemView.item_podcast_subtitle_text_view.text = mediaDescriptionCompat.subtitle

            mediaDescriptionCompat.mediaId?.apply {
                if (this.endsWith(playbackMediaId)) {
                    itemView.item_podcast_icon_image_view.setImageDrawable(animation)
                    animation.start()
                    when (playbackState) {
                        PlaybackStateCompat.STATE_PAUSED -> animation.stop()
                    }
                } else {
                    mediaDescriptionCompat.iconBitmap?.let {
                        itemView.item_podcast_icon_image_view.setImageBitmap(it)
                    } ?: itemView.item_podcast_icon_image_view.setImageResource(R.drawable.ic_podcasts_black_24dp)
                }
                itemView.setOnClickListener { clickListener?.onMediaIdClick(this) }
            }
        }
    }

}