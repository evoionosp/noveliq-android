package org.evoionosp.noveliq.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.evoionosp.noveliq.domain.connection.ConnectionSettingsStore

/**
 * Applies the user's connection settings to every request: custom headers
 * first, then the configured User-Agent (which wins over a custom
 * User-Agent row, matching lissen-android). Reads the current snapshot per
 * request, so header and agent edits apply without rebuilding the client.
 * Installed on both the API and the auth clients: login and server checks
 * traverse the same proxies as everything else.
 */
class ConnectionHeadersInterceptor(
    private val connectionSettingsStore: ConnectionSettingsStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // StateFlow-backed: first() returns the current snapshot without suspending.
        val settings = runBlocking { connectionSettingsStore.settings.first() }
        val request =
            chain
                .request()
                .newBuilder()
                .apply {
                    settings.customHeaders.forEach { header ->
                        // Authorization stays owned by the session: call sites set it
                        // per request and the authenticator replays it on 401. Letting
                        // a custom row win here would silently break auth.
                        if (!header.name.equals(AUTHORIZATION_HEADER, ignoreCase = true)) {
                            header(header.name, header.value)
                        }
                    }
                    header(USER_AGENT_HEADER, settings.userAgent)
                }.build()
        return chain.proceed(request)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val USER_AGENT_HEADER = "User-Agent"
    }
}
