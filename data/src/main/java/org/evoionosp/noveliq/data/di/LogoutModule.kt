package org.evoionosp.noveliq.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.evoionosp.noveliq.data.session.LocalCatalogCleanerImpl
import org.evoionosp.noveliq.data.session.NoOpDownloadStore
import org.evoionosp.noveliq.domain.session.DownloadStore
import org.evoionosp.noveliq.domain.session.LocalCatalogCleaner

@Module
@InstallIn(SingletonComponent::class)
abstract class LogoutModule {
    @Binds
    abstract fun bindLocalCatalogCleaner(impl: LocalCatalogCleanerImpl): LocalCatalogCleaner

    @Binds
    abstract fun bindDownloadStore(impl: NoOpDownloadStore): DownloadStore
}
