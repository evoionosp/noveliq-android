package org.evoionosp.noveliq.data.network

import org.evoionosp.noveliq.data.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object OkHttpProvider {
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * Builds the shared HTTP client.
     *
     * @param authenticator installed for authenticated API traffic so 401s trigger a token
     * refresh and replay. Left null for auth traffic itself (login, server check, token refresh),
     * which must not be able to recurse into a refresh.
     */
    fun create(authenticator: Authenticator? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (authenticator != null) {
            builder.authenticator(authenticator)
        }

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
                .apply {
                    redactHeader("Authorization")
                    redactHeader("x-refresh-token")
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }
}
