package com.softartdev.poder.injection.component

import com.softartdev.poder.injection.PerActivity
import com.softartdev.poder.injection.module.ActivityModule
import com.softartdev.poder.ui.base.BaseActivity
import com.softartdev.poder.ui.detail.DetailActivity
import com.softartdev.poder.ui.pokemon.PokemonActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [ActivityModule::class])
interface ActivityComponent {
    fun inject(baseActivity: BaseActivity)

    fun inject(pokemonActivity: PokemonActivity)

    fun inject(detailActivity: DetailActivity)
}
