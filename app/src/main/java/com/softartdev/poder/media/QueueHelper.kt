/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softartdev.poder.media

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.softartdev.poder.media.MediaPlaybackService.Companion.CATEGORY_SEPARATOR
import com.softartdev.poder.media.MediaPlaybackService.Companion.LEAF_SEPARATOR
import com.softartdev.poder.media.MediaPlaybackService.Companion.MEDIA_ID_PODCAST
import com.softartdev.poder.media.MediaPlaybackService.Companion.MEDIA_ID_ROOT
import timber.log.Timber
import java.util.*

/**
 * Utility class to help on queue related tasks.
 */
object QueueHelper {

    fun getPlayingQueue(mediaId: String, mediaProvider: MediaProvider?): List<MediaSessionCompat.QueueItem>? {
        // This sample only supports genre and by_search category types.
        val tracks: Iterable<MediaMetadataCompat>? = if (mediaId.contains(MEDIA_ID_PODCAST)) {
            mediaProvider?.metadataList
        } else {
            Timber.e("Unrecognized category type for mediaId %s", mediaId)
            null
        }
        return tracks?.let { convertToQueue(it) }
    }

    fun isIndexPlayable(index: Int, queue: List<MediaSessionCompat.QueueItem>?): Boolean
            = queue?.let { index >= 0 && index < it.size } ?: false

    fun getMusicIndexOnQueue(queue: Iterable<MediaSessionCompat.QueueItem>, mediaId: String): Int {
        for ((index, item) in queue.withIndex()) {
            if (mediaId == item.description.mediaId) {
                return index
            }
        }
        return -1
    }

    fun getMusicIndexOnQueue(queue: Iterable<MediaSessionCompat.QueueItem>, queueId: Long): Int {
        for ((index, item) in queue.withIndex()) {
            if (queueId == item.queueId) {
                return index
            }
        }
        return -1
    }

    private fun convertToQueue(tracks: Iterable<MediaMetadataCompat>): List<MediaSessionCompat.QueueItem> {
        val queue = ArrayList<MediaSessionCompat.QueueItem>()
        for ((count, track) in tracks.withIndex()) {
            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
            // at the QueueItem media IDs.
            val hierarchyAwareMediaID = MEDIA_ID_ROOT + CATEGORY_SEPARATOR + MEDIA_ID_PODCAST + LEAF_SEPARATOR + track.description.mediaId
            val duration = track.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            val descriptionBuilder = MediaDescriptionCompat.Builder()
            val description = track.description
            val extras = description.extras ?: Bundle()
            extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            descriptionBuilder.setExtras(extras)
                    .setMediaId(hierarchyAwareMediaID)
                    .setTitle(description.title)
                    .setSubtitle(track.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                    .setIconBitmap(description.iconBitmap)
                    .setIconUri(description.iconUri)
                    .setMediaUri(description.mediaUri)
                    .setDescription(description.description)
            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            val item = MediaSessionCompat.QueueItem(descriptionBuilder.build(), count.toLong())
            queue.add(item)
        }
        return queue
    }
}
