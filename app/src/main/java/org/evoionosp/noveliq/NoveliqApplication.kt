package org.evoionosp.noveliq

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.evoionosp.noveliq.catalog.CatalogSyncCoordinator
import org.evoionosp.noveliq.presentation.utils.newCoverArtInterceptor

@HiltAndroidApp
class NoveliqApplication :
    Application(),
    ImageLoaderFactory {
    @Inject
    lateinit var catalogSyncCoordinator: CatalogSyncCoordinator

    override fun onCreate() {
        super.onCreate()
        catalogSyncCoordinator.start()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components { add(newCoverArtInterceptor()) }
            .build()
}
