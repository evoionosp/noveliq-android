package org.evoionosp.noveliq.data.network

import android.content.Context
import java.util.concurrent.TimeUnit
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.evoionosp.noveliq.data.BuildConfig
import org.evoionosp.noveliq.domain.connection.ConnectionSettingsStore
import org.evoionosp.noveliq.domain.connection.model.ConnectionSettings

object OkHttpProvider {
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * Builds one shared HTTP client. Only [ManagedOkHttpClients] calls this:
     * it owns both clients and rebuilds them when security-relevant
     * connection settings change.
     *
     * @param authenticator installed for authenticated API traffic so 401s trigger a token
     * refresh and replay. Left null for auth traffic itself (login, server check, token refresh),
     * which must not be able to recurse into a refresh.
     * @param connectionSettingsStore read per request by the headers
     * interceptor, so header and agent edits apply without a rebuild.
     * @param settings snapshot of the security-relevant settings baked into
     * this build: SSL bypass and the client certificate alias.
     */
    fun create(
        authenticator: Authenticator?,
        connectionSettingsStore: ConnectionSettingsStore,
        context: Context,
        settings: ConnectionSettings,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(ConnectionHeadersInterceptor(connectionSettingsStore))
                .withConnectionSecurity(
                    bypassSsl = settings.bypassSsl,
                    clientCertificateAlias = settings.clientCertificateAlias,
                    context = context,
                )

        if (authenticator != null) {
            builder.authenticator(authenticator)
        }

        if (BuildConfig.DEBUG) {
            val loggingInterceptor =
                HttpLoggingInterceptor()
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
