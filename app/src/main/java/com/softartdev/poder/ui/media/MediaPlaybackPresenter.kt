package com.softartdev.poder.ui.media

import com.softartdev.poder.data.DataManager
import com.softartdev.poder.injection.ConfigPersistent
import com.softartdev.poder.ui.base.BasePresenter
import javax.inject.Inject

@ConfigPersistent
class MediaPlaybackPresenter @Inject
constructor(private val dataManager: DataManager) : BasePresenter<MediaPlaybackView>() {
    //TODO
}