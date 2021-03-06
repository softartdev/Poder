package com.softartdev.poder.ui.main

import android.support.annotation.IdRes
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.FragmentManager
import android.view.MenuItem
import com.softartdev.poder.R
import com.softartdev.poder.ui.main.downloads.DownloadsFragment
import com.softartdev.poder.ui.main.map.MapFragment
import com.softartdev.poder.ui.main.podcasts.PodcastsFragment
import com.softartdev.poder.ui.main.pokemon.PokemonFragment

class MainNavigation(private var fragmentManager: FragmentManager) : BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        onFragmentSelected(item.itemId)
        return true
    }

    private fun onFragmentSelected(@IdRes itemId: Int) {
        when (itemId) {
            R.id.navigation_pokemon -> showSelectedFragment(POKEMON_TAG)
            R.id.navigation_map -> showSelectedFragment(MAP_TAG)
            R.id.navigation_downloads -> showSelectedFragment(DOWNLOADS_TAG)
            R.id.navigation_podcasts -> showSelectedFragment(PODCASTS_TAG)
        }
    }

    private fun showSelectedFragment(tag: String) {
        var selectedFragment = fragmentManager.findFragmentByTag(tag)
        if (selectedFragment != null) {
            //if the fragment exists, show it.
            fragmentManager.beginTransaction().show(selectedFragment).commit()
        } else {
            //if the fragment does not exist, add it to fragment manager.
            selectedFragment = when (tag) {
                POKEMON_TAG -> PokemonFragment()
                MAP_TAG -> MapFragment()
                DOWNLOADS_TAG -> DownloadsFragment()
                PODCASTS_TAG -> PodcastsFragment()
                else -> PokemonFragment()
            }
            fragmentManager.beginTransaction().add(R.id.main_frame_layout, selectedFragment, tag).commit()
        }
        hideOthers(tag)
    }

    private fun hideOthers(tagSelected: String) {
        //if the other fragments is visible, hide it.
        TAGS.asSequence()
                .filter { it != tagSelected }
                .forEach { hideUnselectedFragment(it) }
    }

    private fun hideUnselectedFragment(tag: String) {
        val unselectedFragment = fragmentManager.findFragmentByTag(tag)
        if (unselectedFragment != null) {
            fragmentManager.beginTransaction().hide(unselectedFragment).commit()
        }
    }

    companion object {
        private const val POKEMON_TAG = "pokemon_tag"
        private const val MAP_TAG = "map_tag"
        private const val DOWNLOADS_TAG = "downloads_tag"
        private const val PODCASTS_TAG = "podcasts_tag"
        private val TAGS = arrayOf(POKEMON_TAG, MAP_TAG, DOWNLOADS_TAG, PODCASTS_TAG)
    }
}
