package com.softartdev.poder.injection.component

import com.softartdev.poder.injection.PerFragment
import com.softartdev.poder.injection.module.FragmentModule
import dagger.Subcomponent

/**
 * This component inject dependencies to all Fragments across the application
 */
@PerFragment
@Subcomponent(modules = [FragmentModule::class])
interface FragmentComponent