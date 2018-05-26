package com.softartdev.poder.media

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.softartdev.poder.injection.ApplicationContext
import com.softartdev.poder.util.ViewUtil
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class MediaProvider(@ApplicationContext private val context: Context) {
    private val defaultArtwork = ViewUtil.getDefaultAlbumArt(context)
    val metadataList: MutableList<MediaMetadataCompat> = mutableListOf()
    private var podcastList: List<MediaBrowserCompat.MediaItem>? = null
    private val mediaListById: ConcurrentMap<String, Podcast> = ConcurrentHashMap()
    @Volatile private var currentState = State.NON_INITIALIZED

    val isInitialized: Boolean
        get() = currentState == State.INITIALIZED

    /**
     * Return the MediaMetadataCompat for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    fun getMediaById(musicId: String?): Podcast? {
        return if (mediaListById.containsKey(musicId)) mediaListById[musicId] else null
    }

    var podcasts: List<MediaBrowserCompat.MediaItem>?
        get() {
            if (podcastList == null) {
                currentState = State.INITIALIZING
                val cursor: Cursor = context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null,
                        MediaStore.Audio.Media.IS_PODCAST,
                        null,
                        MediaStore.Audio.Media.TITLE + " ASC"
                ) ?: throw IllegalStateException("Failed to retrieve music: cursor is null")

                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    do {
                        val thisId = cursor.getLong(idColumn)
                        Timber.i("Media ID: %s Title: %s", thisId, cursor.getString(titleColumn))
                        val thisPath = cursor.getString(pathColumn)
                        val metadata = retrieveMetadata(thisId, thisPath) ?: continue
                        Timber.i("MediaMetadataCompat: %s", metadata)
                        metadataList.add(metadata)
                        mediaListById[thisId.toString()] = Podcast(thisId, metadata, null)
                    } while (cursor.moveToNext())
                    cursor.close()
                } else {
                    cursor.close()
                    throw IllegalStateException("Failed to move cursor to first row (no query result)")
                }

                val mediaList: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()

                val parentId = MEDIA_ID_MUSICS_BY_SONG + CATEGORY_SEPARATOR + MEDIA_ID_MUSICS_BY_SONG
                for (metadata in metadataList) {
                    val item = loadMedia(parentId, metadata)
                    mediaList.add(item)
                }
                podcastList = mediaList
                currentState = State.INITIALIZED
            }
            return podcastList
        }
        set(value) {
            metadataList.clear()
            currentState = State.NON_INITIALIZED
            podcastList = value
        }

    @Synchronized
    private fun retrieveMetadata(mediaId: Long, mediaPath: String): MediaMetadataCompat? {
        val contentUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)
        return if (File(mediaPath).exists()) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, contentUri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val duration: Long = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLongOrNull() ?: 0

            val embedded: Bitmap? = retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            val bitmap: Bitmap? = embedded?.let { Bitmap.createScaledBitmap(it, defaultArtwork.width, defaultArtwork.height, false) }

            retriever.release()
            with(MediaMetadataCompat.Builder()) {
                putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId.toString())
                putString(CUSTOM_METADATA_TRACK_SOURCE, mediaPath)
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, title ?: UNKNOWN)
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album ?: UNKNOWN)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist ?: UNKNOWN)
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                build()
            }
        } else {
            Timber.d("Does not exist, deleting item")
            context.contentResolver.delete(contentUri, null, null)
            null
        }
    }

    private fun loadMedia(parentId: String, metadata: MediaMetadataCompat): MediaBrowserCompat.MediaItem {
        val hierarchyAwareMediaID = createMediaID(metadata.description.mediaId, parentId)
        val songExtra = Bundle()
        songExtra.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
        val title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        val artistName = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        val artwork = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
        val description = with(MediaDescriptionCompat.Builder()) {
            setMediaId(hierarchyAwareMediaID)
            setTitle(title)
            setSubtitle(artistName)
            setIconBitmap(artwork)
            setExtras(songExtra)
            build()
        }
        return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    private fun createMediaID(musicID: String?, vararg categories: String?): String {
        return with(StringBuilder()) {
            for ((index, category) in categories.withIndex()) {
                if (index > 0) append(CATEGORY_SEPARATOR)
                append(category)
            }
            musicID?.let { append(LEAF_SEPARATOR).append(it) }
            toString()
        }
    }

    internal enum class State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    companion object {
        const val UNKNOWN = "UNKNOWN"
        const val CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__"
        private const val CATEGORY_SEPARATOR: Char = 31.toChar()
        private const val MEDIA_ID_MUSICS_BY_SONG = "__BY_SONG__" // parent id
        private const val LEAF_SEPARATOR: Char = 30.toChar()
    }
}