package org.evoionosp.noveliq.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.evoionosp.noveliq.core.settings.AppSettingsDataStore
import org.evoionosp.noveliq.core.settings.AppSettingsStore

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    fun provideAppSettingsStore(
        @ApplicationContext context: Context
    ): AppSettingsStore {
        return AppSettingsDataStore(context)
    }
}
