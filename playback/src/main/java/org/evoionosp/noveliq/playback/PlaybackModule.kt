package org.evoionosp.noveliq.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
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
import org.evoionosp.noveliq.domain.session.SessionStore

@Module
@InstallIn(ServiceComponent::class)
object PlaybackModule {

    @OptIn(UnstableApi::class)
    @Provides
    @ServiceScoped
    fun provideDataSourceFactory(
        sessionStore: SessionStore
    ): DataSource.Factory {
        // Resolve the Authorization header per request so streaming (and notification artwork)
        // always use the CURRENT session token. The token is refreshed/rotated on app start, so a
        // token captured once at service creation goes stale and causes 401s.
        val upstreamFactory = DefaultHttpDataSource.Factory()
        return ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
            val token = runBlocking { sessionStore.session.first()?.accessToken.orEmpty() }
            dataSpec.withRequestHeaders(mapOf("Authorization" to "Bearer $token"))
        }
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
