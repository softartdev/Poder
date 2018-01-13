package com.softartdev.poder.ui.pokemon

import com.softartdev.poder.data.DataManager
import com.softartdev.poder.ui.base.BasePresenter
import com.softartdev.poder.injection.ConfigPersistent
import com.softartdev.poder.util.rx.scheduler.SchedulerUtils
import javax.inject.Inject

@ConfigPersistent
class PokemonPresenter @Inject
constructor(private val dataManager: DataManager) : BasePresenter<PokemonMvpView>() {

    fun getPokemon(limit: Int) {
        checkViewAttached()
        mvpView?.showProgress(true)
        dataManager.getPokemonList(limit)
                .compose(SchedulerUtils.ioToMain<List<String>>())
                .subscribe({ pokemons ->
                    mvpView?.apply {
                        showProgress(false)
                        showPokemon(pokemons)
                    }
                }) { throwable ->
                    mvpView?.apply {
                        showProgress(false)
                        showError(throwable)
                    }
                }
    }
}