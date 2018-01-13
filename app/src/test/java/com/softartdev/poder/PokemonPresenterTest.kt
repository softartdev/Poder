package com.softartdev.poder

import com.nhaarman.mockito_kotlin.*
import com.softartdev.poder.common.TestDataFactory
import com.softartdev.poder.data.DataManager
import com.softartdev.poder.features.pokemon.PokemonMvpView
import com.softartdev.poder.features.pokemon.PokemonPresenter
import com.softartdev.poder.util.RxSchedulersOverrideRule
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.junit.MockitoJUnitRunner

/**
 * Created by ravindra on 24/12/16.
 */
@RunWith(MockitoJUnitRunner::class)
class PokemonPresenterTest {

    val pokemonList = TestDataFactory.makePokemonNamesList(10)

    val mMockPokemonMvpView: PokemonMvpView = mock()
    val mockDataManager: DataManager = mock {
        on { getPokemonList(10) } doReturn Single.just(pokemonList)
        on { getPokemonList(5) } doReturn Single.error<List<String>>(RuntimeException())
    }
    private var mPokemonPresenter: PokemonPresenter? = null

    @JvmField
    @Rule
    val overrideSchedulersRule = RxSchedulersOverrideRule()

    @Before
    fun setUp() {
        mPokemonPresenter = PokemonPresenter(mockDataManager)
        mPokemonPresenter?.attachView(mMockPokemonMvpView)
    }

    @After
    fun tearDown() {
        mPokemonPresenter?.detachView()
    }

    @Test
    @Throws(Exception::class)
    fun getPokemonReturnsPokemonNames() {

        mPokemonPresenter?.getPokemon(10)

        verify(mMockPokemonMvpView, times(2)).showProgress(anyBoolean())
        verify(mMockPokemonMvpView).showPokemon(pokemonList)
        verify(mMockPokemonMvpView, never()).showError(RuntimeException())

    }

    @Test
    @Throws(Exception::class)
    fun getPokemonReturnsError() {

        mPokemonPresenter?.getPokemon(5)

        verify(mMockPokemonMvpView, times(2)).showProgress(anyBoolean())
        verify(mMockPokemonMvpView).showError(any())
        verify(mMockPokemonMvpView, never()).showPokemon(any())
    }
}