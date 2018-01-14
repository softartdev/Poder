package com.softartdev.poder.ui.pokemon

import com.softartdev.poder.ui.base.MvpView

interface PokemonMvpView : MvpView {

    fun showPokemon(pokemon: List<String>)

    fun showProgress(show: Boolean)

    fun showError(error: Throwable)

}