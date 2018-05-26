package com.softartdev.poder.media

import org.junit.Test

import org.junit.Assert.*

class MediaUtilsTest {

    @Test
    fun removeMediaIdPrefix() {
        val mediaId = "51"
        val podcastId = MediaPlaybackService.MEDIA_ID_ROOT + MediaPlaybackService.CATEGORY_SEPARATOR + MediaPlaybackService.MEDIA_ID_PODCAST + MediaPlaybackService.LEAF_SEPARATOR + mediaId
        val id = MediaUtils.removeMediaIdPrefix(podcastId)
        assertEquals(mediaId, id)
    }
}