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
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.evoionosp.noveliq.domain.session.PlayerLogoutHandler
import org.evoionosp.noveliq.domain.session.SessionStore

internal const val AUTHORIZATION_HEADER = "Authorization"

/** The current access token, or empty when signed out (requests then go out unauthenticated). */
internal suspend fun SessionStore.currentAccessToken(): String = session.first()?.accessToken.orEmpty()

/** The per-request auth headers for streaming and notification artwork fetches. */
internal fun authorizationHeaders(accessToken: String): Map<String, String> =
    mapOf(AUTHORIZATION_HEADER to "Bearer $accessToken")

@Module
@InstallIn(ServiceComponent::class)
object PlaybackModule {
    @OptIn(UnstableApi::class)
    @Provides
    @ServiceScoped
    fun provideDataSourceFactory(sessionStore: SessionStore): DataSource.Factory {
        // Resolve the Authorization header per request so streaming (and notification artwork)
        // always use the CURRENT session token. The token is refreshed/rotated on app start, so a
        // token captured once at service creation goes stale and causes 401s.
        val upstreamFactory = DefaultHttpDataSource.Factory()
        return ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
            val token = runBlocking { sessionStore.currentAccessToken() }
            dataSpec.withRequestHeaders(authorizationHeaders(token))
        }
    }

    @OptIn(UnstableApi::class)
    @Provides
    @ServiceScoped
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        dataSourceFactory: DataSource.Factory,
    ): ExoPlayer =
        ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
}

/**
 * Application-lifetime bindings for [PlaybackConnection]. Lives in [SingletonComponent] (not the
 * service component above) because the connection itself is a singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object PlaybackConnectionBindings {
    @Provides
    @Singleton
    fun provideConnectionScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    @Singleton
    fun provideControllerConnector(impl: SessionMediaControllerConnector): MediaControllerConnector = impl

    @Provides
    @Singleton
    fun providePlayerLogoutHandler(impl: PlaybackConnectionLogoutHandler): PlayerLogoutHandler = impl
}
