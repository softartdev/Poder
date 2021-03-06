package com.softartdev.poder.ui.pokemon

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.softartdev.poder.R
import com.softartdev.poder.ui.base.BaseActivity
import com.softartdev.poder.ui.common.ErrorView
import com.softartdev.poder.ui.detail.DetailActivity
import com.softartdev.poder.util.gone
import com.softartdev.poder.util.visible
import kotlinx.android.synthetic.main.activity_pokemon.*
import timber.log.Timber
import javax.inject.Inject


class PokemonActivity(override val layout: Int = R.layout.activity_pokemon) : BaseActivity(), PokemonMvpView, PokemonAdapter.ClickListener, ErrorView.ErrorListener {

    @Inject lateinit var pokemonAdapter: PokemonAdapter
    @Inject lateinit var mPokemonPresenter: PokemonPresenter

    companion object {
        const val POKEMON_COUNT = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent().inject(this)
        mPokemonPresenter.attachView(this)

        setSupportActionBar(pokemon_toolbar)
        swipeToRefresh?.apply {
            setProgressBackgroundColorSchemeResource(R.color.primary)
            setColorSchemeResources(R.color.white)
            setOnRefreshListener { mPokemonPresenter.getPokemon(POKEMON_COUNT) }
        }

        pokemonAdapter.setClickListener(this)
        recyclerPokemon?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pokemonAdapter
        }

        viewError?.setErrorListener(this)

        mPokemonPresenter.getPokemon(POKEMON_COUNT)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPokemonPresenter.detachView()
    }

    override fun showPokemon(pokemon: List<String>) {
        pokemonAdapter.apply {
            setPokemon(pokemon)
            notifyDataSetChanged()
        }

        recyclerPokemon?.visible()
        swipeToRefresh?.visible()
    }

    override fun showProgress(show: Boolean) {
        if (show) {
            if (recyclerPokemon?.visibility == View.VISIBLE && pokemonAdapter.itemCount > 0) {
                swipeToRefresh?.isRefreshing = true
            } else {
                progressBar?.visible()
                recyclerPokemon?.gone()
                swipeToRefresh?.gone()
            }

            viewError?.gone()
        } else {
            swipeToRefresh?.isRefreshing = false
            progressBar?.gone()
        }
    }

    override fun showError(error: Throwable) {
        recyclerPokemon?.gone()
        swipeToRefresh?.gone()
        viewError?.visible()
        Timber.e(error, "There was an error retrieving the pokemon")
    }

    override fun onPokemonClick(pokemon: String) {
        startActivity(DetailActivity.getStartIntent(this, pokemon))
    }

    override fun onReloadData() {
        mPokemonPresenter.getPokemon(POKEMON_COUNT)
    }

}