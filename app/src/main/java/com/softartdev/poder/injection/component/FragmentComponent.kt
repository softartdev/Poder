package com.softartdev.poder.injection.component

import com.softartdev.poder.injection.PerFragment
import com.softartdev.poder.injection.module.FragmentModule
import com.softartdev.poder.ui.main.downloads.DownloadsFragment
import com.softartdev.poder.ui.main.podcasts.PodcastsFragment
import com.softartdev.poder.ui.main.pokemon.PokemonFragment
import dagger.Subcomponent

/**
 * This component inject dependencies to all Fragments across the application
 */
@PerFragment
@Subcomponent(modules = [FragmentModule::class])
interface FragmentComponent {
    fun inject(pokemonFragment: PokemonFragment)
    fun inject(downloadsFragment: DownloadsFragment)
    fun inject(podcastsFragment: PodcastsFragment)
}