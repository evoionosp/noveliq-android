package org.evoionosp.noveliq

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.Call
import org.evoionosp.noveliq.catalog.CatalogSyncCoordinator
import org.evoionosp.noveliq.data.network.ManagedOkHttpClients
import org.evoionosp.noveliq.presentation.utils.newCoverArtInterceptor

@HiltAndroidApp
class NoveliqApplication :
    Application(),
    ImageLoaderFactory {
    @Inject
    lateinit var catalogSyncCoordinator: CatalogSyncCoordinator

    @Inject
    lateinit var httpClients: ManagedOkHttpClients

    override fun onCreate() {
        super.onCreate()
        catalogSyncCoordinator.start()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components { add(newCoverArtInterceptor()) }
            // Covers traverse the same proxies as everything else, so they
            // share the connection-aware client. Resolved per call so TLS
            // rebuilds apply without recreating the loader.
            .callFactory(Call.Factory { request -> httpClients.apiClient.newCall(request) })
            .build()
}
