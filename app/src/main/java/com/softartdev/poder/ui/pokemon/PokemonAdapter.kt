package com.softartdev.poder.ui.pokemon

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.softartdev.poder.R
import com.softartdev.poder.injection.ConfigPersistent
import javax.inject.Inject

@ConfigPersistent
class PokemonAdapter @Inject
constructor() : RecyclerView.Adapter<PokemonAdapter.PokemonViewHolder>() {

    private var pokemonsList: List<String>
    private var clickListener: ClickListener? = null

    init {
        pokemonsList = emptyList()
    }

    fun setPokemon(pokemons: List<String>) {
        pokemonsList = pokemons
    }

    fun setClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
        val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_pokemon, parent, false)
        return PokemonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) {
        val pokemon = pokemonsList[position]
        holder.bind(pokemon)
    }

    override fun getItemCount(): Int {
        return pokemonsList.size
    }

    interface ClickListener {
        fun onPokemonClick(pokemon: String)
    }

    inner class PokemonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private lateinit var selectedPokemon: String

        @BindView(R.id.pokemon_name)
        @JvmField var pokemonName: TextView? = null

        init {
            ButterKnife.bind(this, itemView)
            itemView.setOnClickListener {
                clickListener?.onPokemonClick(selectedPokemon)
            }
        }

        fun bind(pokemon: String) {
            selectedPokemon = pokemon
            pokemonName?.text = String.format("%s%s", pokemon.substring(0, 1).toUpperCase(),
                    pokemon.substring(1))
        }
    }

}