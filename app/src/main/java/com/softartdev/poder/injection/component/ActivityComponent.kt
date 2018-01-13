package com.softartdev.poder.injection.component

import com.softartdev.poder.injection.PerActivity
import com.softartdev.poder.injection.module.ActivityModule
import com.softartdev.poder.features.base.BaseActivity
import com.softartdev.poder.features.detail.DetailActivity
import com.softartdev.poder.features.pokemon.MainActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [ActivityModule::class])
interface ActivityComponent {
    fun inject(baseActivity: BaseActivity)

    fun inject(mainActivity: MainActivity)

    fun inject(detailActivity: DetailActivity)
}
