package org.evoionosp.noveliq.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.evoionosp.noveliq.core.session.SessionDataStore
import org.evoionosp.noveliq.core.session.SessionStore

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun provideSessionDataStore(
        @ApplicationContext context: Context
    ): SessionDataStore {
        return SessionDataStore(context)
    }

    @Provides
    @Singleton
    fun provideSessionStore(sessionDataStore: SessionDataStore): SessionStore = sessionDataStore
}
