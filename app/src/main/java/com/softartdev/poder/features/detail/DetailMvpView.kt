package com.softartdev.poder.features.detail

import com.softartdev.poder.data.model.Pokemon
import com.softartdev.poder.data.model.Statistic
import com.softartdev.poder.features.base.MvpView

interface DetailMvpView : MvpView {

    fun showPokemon(pokemon: Pokemon)

    fun showStat(statistic: Statistic)

    fun showProgress(show: Boolean)

    fun showError(error: Throwable)

}