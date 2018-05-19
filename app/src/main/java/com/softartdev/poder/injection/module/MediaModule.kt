package com.softartdev.poder.injection.module

import android.content.Context
import com.softartdev.poder.injection.ApplicationContext
import com.softartdev.poder.media.MediaProvider
import dagger.Module
import dagger.Provides

@Module
class MediaModule {

    @Provides
    internal fun provideMediaProvider(@ApplicationContext context: Context): MediaProvider = MediaProvider(context)

}