package com.softartdev.poder.injection.component

import android.app.Application
import android.content.Context
import com.softartdev.poder.data.DataManager
import com.softartdev.poder.data.remote.PokemonApi
import com.softartdev.poder.injection.ApplicationContext
import com.softartdev.poder.injection.module.AppModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    @ApplicationContext
    fun context(): Context

    fun application(): Application

    fun dataManager(): DataManager

    fun pokemonApi(): PokemonApi
}
