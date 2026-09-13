package org.evoionosp.noveliq.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.data.network.OkHttpProvider
import org.evoionosp.noveliq.data.network.TokenAuthenticator

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** Qualifier for the client used by auth traffic itself: login, server check, token refresh. */
    const val AUTH_CLIENT = "auth"

    /**
     * Client for authenticated API traffic. Its authenticator rotates the access token and
     * replays the request when the server answers 401.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpProvider.create(authenticator = tokenAuthenticator)
    }

    /**
     * Client without the authenticator, for the calls that establish or renew a session.
     *
     * Three reasons it has to be separate: a 401 from login or refresh means "these credentials
     * are wrong", not "retry with a new token"; sharing one client would make the object graph
     * circular, since the authenticator needs the auth repository which needs a client; and the
     * authenticator blocks its calling thread while refreshing, so the refresh has to run on a
     * different client's thread pool than the request that triggered it.
     */
    @Provides
    @Singleton
    @Named(AUTH_CLIENT)
    fun provideAuthOkHttpClient(): OkHttpClient {
        return OkHttpProvider.create()
    }
}
