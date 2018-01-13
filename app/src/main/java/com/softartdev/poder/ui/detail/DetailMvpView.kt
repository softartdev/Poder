package com.softartdev.poder.ui.detail

import com.softartdev.poder.data.model.Pokemon
import com.softartdev.poder.data.model.Statistic
import com.softartdev.poder.ui.base.MvpView

interface DetailMvpView : MvpView {

    fun showPokemon(pokemon: Pokemon)

    fun showStat(statistic: Statistic)

    fun showProgress(show: Boolean)

    fun showError(error: Throwable)

}