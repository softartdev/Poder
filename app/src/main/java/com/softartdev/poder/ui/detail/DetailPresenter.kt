package com.softartdev.poder.ui.detail

import com.softartdev.poder.data.DataManager
import com.softartdev.poder.injection.ConfigPersistent
import com.softartdev.poder.ui.base.BasePresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ConfigPersistent
class DetailPresenter @Inject
constructor(private val dataManager: DataManager) : BasePresenter<DetailMvpView>() {

    fun getPokemon(name: String) {
        checkViewAttached()
        mvpView?.showProgress(true)
        addDisposable(dataManager.getPokemon(name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pokemon ->
                    // It should be always checked if MvpView (Fragment or Activity) is attached.
                    // Calling showProgress() on a not-attached fragment will throw a NPE
                    // It is possible to ask isAdded() in the fragment, but it's better to ask in the presenter
                    mvpView?.apply {
                        showProgress(false)
                        showPokemon(pokemon)
                        for (statistic in pokemon.stats) {
                            showStat(statistic)
                        }
                    }
                }) { throwable ->
                    mvpView?.apply {
                        showProgress(false)
                        showError(throwable)
                    }
                })
    }
}