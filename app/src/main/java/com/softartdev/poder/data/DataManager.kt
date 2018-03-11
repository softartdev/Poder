package com.softartdev.poder.data

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.softartdev.poder.data.model.Pokemon
import com.softartdev.poder.data.remote.PokemonApi
import com.softartdev.poder.injection.ApplicationContext
import com.softartdev.poder.util.ViewUtil
import io.reactivex.Single
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject
constructor(private val pokemonApi: PokemonApi, @ApplicationContext private val context: Context) {

    private val defaultArtwork = ViewUtil.getDefaultAlbumArt(context)

    fun getPokemonList(limit: Int): Single<List<String>> {
        return pokemonApi.getPokemonList(limit)
                .toObservable()
                .flatMapIterable { (results) -> results }
                .map { (name) -> name }
                .toList()
    }

    fun getPokemon(name: String): Single<Pokemon> {
        return pokemonApi.getPokemon(name)
    }

    fun getDownloads(): Single<Array<File>> {
        return Single.fromCallable {
            val dirType = Environment.DIRECTORY_DOWNLOADS
            val dirDownloads = Environment.getExternalStoragePublicDirectory(dirType)

            dirDownloads
                    ?: throw IllegalStateException("Failed to get external storage public directory")
            if (dirDownloads.exists()) {
                if (!dirDownloads.isDirectory) {
                    throw IllegalStateException(dirDownloads.absolutePath + " already exists and is not a directory")
                }
            } else {
                if (!dirDownloads.mkdirs()) {
                    throw IllegalStateException("Unable to create directory: " + dirDownloads.absolutePath)
                }
            }

            dirDownloads.listFiles() ?: arrayOfNulls(0)
        }
    }

    @SuppressLint("Recycle")
    fun getPodcasts(): Single<List<MediaBrowserCompat.MediaItem>> = Single.fromCallable {
        val cursor: Cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_PODCAST,
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
        ) ?: throw IllegalStateException("Failed to retrieive music: cursor is null")

        val metadataList: MutableList<MediaMetadataCompat> = mutableListOf()

        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            do {
                Timber.i("Media ID: %s Title: %s", cursor.getString(idColumn), cursor.getString(titleColumn))
                val thisId = cursor.getLong(idColumn)
                val thisPath = cursor.getString(pathColumn)
                val metadata = retrieveMetadata(thisId, thisPath) ?: continue
                Timber.i("MediaMetadataCompat: %s", metadata)
                metadataList.add(metadata)
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
        mediaList
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
            val bitmap: Bitmap? = Bitmap.createScaledBitmap(embedded, defaultArtwork.width, defaultArtwork.height, false)

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
        val description = with (MediaDescriptionCompat.Builder()) {
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

    companion object {
        private const val UNKNOWN = "UNKNOWN"
        private const val CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__"
        private const val CATEGORY_SEPARATOR: Char = 31.toChar()
        private const val MEDIA_ID_MUSICS_BY_SONG = "__BY_SONG__" // parent id
        private const val LEAF_SEPARATOR: Char = 30.toChar()
    }

}