package com.softartdev.poder.common.injection.component

import dagger.Component
import com.softartdev.poder.common.injection.module.ApplicationTestModule
import com.softartdev.poder.injection.component.AppComponent
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(ApplicationTestModule::class))
interface TestComponent : AppComponent