package com.softartdev.poder.media

import android.content.Context
import com.softartdev.poder.R
import java.util.*

object MediaUtils {
    /**
     *  Try to use String.format() as little as possible, because it creates a
     *  new Formatter every time you call it, which is very inefficient.
     *  Reusing an existing Formatter more than tripled the speed of
     *  makeTimeString().
     *  This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
     */
    private val sFormatBuilder = StringBuilder()
    private val sFormatter = Formatter(sFormatBuilder, Locale.getDefault())
    private val sTimeArgs = arrayOfNulls<Any>(5)

    fun makeTimeString(context: Context, secs: Long): String {
        val durationFormat = context.getString(
                if (secs < 3600) R.string.duration_format_short else R.string.duration_format_long)

        /* Provide multiple arguments so the format can be changed easily
         * by modifying the xml.
         */
        sFormatBuilder.setLength(0)

        val timeArgs = sTimeArgs
        timeArgs[0] = secs / 3600
        timeArgs[1] = secs / 60
        timeArgs[2] = secs / 60 % 60
        timeArgs[3] = secs
        timeArgs[4] = secs % 60

        return sFormatter.format(durationFormat, *timeArgs).toString()
    }

    fun removeMediaIdPrefix(mediaId: String): String = mediaId
            .removePrefix(MediaPlaybackService.MEDIA_ID_ROOT)
            .removePrefix(MediaPlaybackService.CATEGORY_SEPARATOR.toString())
            .removePrefix(MediaPlaybackService.MEDIA_ID_PODCAST)
            .removePrefix(MediaPlaybackService.LEAF_SEPARATOR.toString())
}