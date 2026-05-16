package org.evoionosp.noveliq

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.evoionosp.noveliq.catalog.CatalogSyncCoordinator

@HiltAndroidApp
class NoveliqApplication: Application() {
    @Inject
    lateinit var catalogSyncCoordinator: CatalogSyncCoordinator

    override fun onCreate() {
        super.onCreate()
        catalogSyncCoordinator.start()
    }
}
