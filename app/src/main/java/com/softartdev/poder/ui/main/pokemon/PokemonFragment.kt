package com.softartdev.poder.ui.main.pokemon

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.softartdev.poder.R
import com.softartdev.poder.ui.base.BaseFragment
import com.softartdev.poder.ui.common.ErrorView
import com.softartdev.poder.ui.detail.DetailActivity
import com.softartdev.poder.ui.pokemon.PokemonActivity
import com.softartdev.poder.ui.pokemon.PokemonAdapter
import com.softartdev.poder.ui.pokemon.PokemonMvpView
import com.softartdev.poder.ui.pokemon.PokemonPresenter
import com.softartdev.poder.util.gone
import com.softartdev.poder.util.visible
import kotlinx.android.synthetic.main.fragment_pokemon.*
import timber.log.Timber
import javax.inject.Inject

class PokemonFragment : BaseFragment(), PokemonMvpView, PokemonAdapter.ClickListener, ErrorView.ErrorListener {

    @Inject lateinit var pokemonAdapter: PokemonAdapter
    @Inject lateinit var pokemonPresenter: PokemonPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentComponent().inject(this)
        pokemonPresenter.attachView(this)
        pokemonAdapter.setClickListener(this)
    }

    override fun layoutId() = R.layout.fragment_pokemon

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pokemon_swipe_refresh?.apply {
            setProgressBackgroundColorSchemeResource(R.color.primary)
            setColorSchemeResources(R.color.white)
            setOnRefreshListener { pokemonPresenter.getPokemon(PokemonActivity.POKEMON_COUNT) }
        }

        pokemonAdapter.setClickListener(this)
        pokemon_recycler_view?.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(pokemon_recycler_view.context, DividerItemDecoration.VERTICAL))
            adapter = pokemonAdapter
        }

        pokemon_error_view?.setErrorListener(this)

        if (pokemonAdapter.itemCount == 0) {
            pokemonPresenter.getPokemon(PokemonActivity.POKEMON_COUNT)
        }
    }

    override fun showPokemon(pokemon: List<String>) {
        pokemonAdapter.apply {
            setPokemon(pokemon)
            notifyDataSetChanged()
        }
    }

    override fun onPokemonClick(pokemon: String) {
        startActivity(activity?.let { DetailActivity.getStartIntent(it, pokemon) })
    }

    override fun showProgress(show: Boolean) {
        if (pokemon_swipe_refresh.isRefreshing) {
            pokemon_swipe_refresh.isRefreshing = show
        } else {
            pokemon_progress_view.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun showError(error: Throwable) {
        pokemon_error_view?.visible()
        Timber.e(error, "There was an error retrieving the pokemon")
    }

    override fun onReloadData() {
        pokemon_error_view?.gone()
        pokemonPresenter.getPokemon(PokemonActivity.POKEMON_COUNT)
    }

    override fun onDestroy() {
        super.onDestroy()
        pokemonPresenter.detachView()
    }

}
