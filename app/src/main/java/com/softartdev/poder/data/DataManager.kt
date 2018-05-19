package com.softartdev.poder.data

import android.os.Environment
import android.support.v4.media.MediaBrowserCompat
import com.softartdev.poder.data.model.Pokemon
import com.softartdev.poder.data.remote.PokemonApi
import com.softartdev.poder.media.MediaProvider
import io.reactivex.Single
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject
constructor(private val pokemonApi: PokemonApi, private val mediaProvider: MediaProvider) {

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
            dirDownloads ?: throw IllegalStateException("Failed to get external storage public directory")
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

    fun getPodcasts(): Single<List<MediaBrowserCompat.MediaItem>> = Single.fromCallable { mediaProvider.podcasts }

    fun updatePodcasts(): Single<List<MediaBrowserCompat.MediaItem>> = Single.fromCallable {
        mediaProvider.podcasts = null
        mediaProvider.podcasts
    }

}