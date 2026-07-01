package org.evoionosp.noveliq.presentation.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.evoionosp.noveliq.core.session.SessionStore

@Module
@InstallIn(ServiceComponent::class)
object PlaybackModule {

    @OptIn(UnstableApi::class)
    @Provides
    @ServiceScoped
    fun provideDataSourceFactory(
        sessionStore: SessionStore
    ): DataSource.Factory {
        val session = runBlocking { sessionStore.session.first() }
        val token = session?.accessToken.orEmpty()
        
        return DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
    }

    @OptIn(UnstableApi::class)
    @Provides
    @ServiceScoped
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        dataSourceFactory: DataSource.Factory
    ): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }
}
