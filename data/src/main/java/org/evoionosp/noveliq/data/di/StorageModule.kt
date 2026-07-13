package org.evoionosp.noveliq.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.evoionosp.noveliq.data.session.SessionDataStore
import org.evoionosp.noveliq.data.settings.AppSettingsDataStore
import org.evoionosp.noveliq.domain.session.SessionStore
import org.evoionosp.noveliq.domain.settings.AppSettingsStore

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideSessionStore(
        @ApplicationContext context: Context
    ): SessionStore = SessionDataStore(context)

    @Provides
    @Singleton
    fun provideAppSettingsStore(
        @ApplicationContext context: Context
    ): AppSettingsStore = AppSettingsDataStore(context)
}
