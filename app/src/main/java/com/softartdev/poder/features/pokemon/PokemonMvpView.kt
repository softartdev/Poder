package com.softartdev.poder.features.pokemon

import com.softartdev.poder.features.base.MvpView

interface PokemonMvpView : MvpView {

    fun showPokemon(pokemon: List<String>)

    fun showProgress(show: Boolean)

    fun showError(error: Throwable)

}