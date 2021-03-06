package com.softartdev.poder.injection.module

import android.app.Application
import android.content.Context
import com.softartdev.poder.injection.ApplicationContext
import dagger.Module
import dagger.Provides

@Module(includes = [ApiModule::class, MediaModule::class])
class AppModule(private val application: Application) {

    @Provides
    internal fun provideApplication(): Application {
        return application
    }

    @Provides
    @ApplicationContext
    internal fun provideContext(): Context {
        return application
    }
}