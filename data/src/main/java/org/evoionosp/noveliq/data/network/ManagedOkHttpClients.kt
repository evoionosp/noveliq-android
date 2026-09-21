package org.evoionosp.noveliq.data.network

import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.evoionosp.noveliq.domain.connection.ConnectionSettingsStore
import org.evoionosp.noveliq.domain.connection.model.ConnectionSettings

/**
 * Owns the two shared HTTP clients and rebuilds them when
 * security-relevant connection settings change.
 *
 * Two clients exist because their 401 semantics differ: the API client
 * carries the authenticator, which rotates the access token and replays the
 * request; the auth client (login, server check, token refresh) has none,
 * since a 401 there means "wrong credentials", not "retry with a new
 * token". Sharing one client would also make the object graph circular,
 * since the authenticator needs the auth repository which needs a client.
 *
 * Only SSL bypass and the client certificate alias trigger a rebuild —
 * custom headers and the User-Agent are read per request by
 * [ConnectionHeadersInterceptor], so they apply with no rebuild at all.
 * Retrofit service factories hold the built clients indirectly and
 * register cache invalidators, so a rebuild never leaves a stale client
 * pinned behind a cached service.
 *
 * The authenticator is [Lazy] to keep Dagger's graph acyclic: the holder
 * needs it to build the API client, while the authenticator transitively
 * needs the holder through the auth repository and its service factory.
 * Clients build lazily on first access, never in init, so the lazy edge is
 * only ever resolved after this instance exists.
 */
@Singleton
class ManagedOkHttpClients
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val connectionSettingsStore: ConnectionSettingsStore,
        private val tokenAuthenticator: Lazy<TokenAuthenticator>,
        @param:Named("io") ioDispatcher: CoroutineDispatcher,
    ) {
        private data class ClientPair(
            val api: OkHttpClient,
            val auth: OkHttpClient,
        )

        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

        @Volatile
        private var current: ClientPair? = null
        private val invalidators = CopyOnWriteArrayList<() -> Unit>()

        init {
            scope.launch {
                connectionSettingsStore.settings
                    .distinctUntilChangedBy { it.bypassSsl to it.clientCertificateAlias }
                    .collect { rebuild(it) }
            }
        }

        val apiClient: OkHttpClient
            get() = ensureCurrent().api

        val authClient: OkHttpClient
            get() = ensureCurrent().auth

        /** Registers a cache clear to run after every client rebuild. */
        fun registerInvalidator(invalidator: () -> Unit) {
            invalidators.add(invalidator)
        }

        @Synchronized
        private fun ensureCurrent(): ClientPair =
            // StateFlow-backed: first() returns the current snapshot without suspending.
            current ?: rebuild(runBlocking { connectionSettingsStore.settings.first() })

        @Synchronized
        private fun rebuild(settings: ConnectionSettings): ClientPair {
            val pair =
                ClientPair(
                    api =
                        OkHttpProvider.create(
                            authenticator = tokenAuthenticator.get(),
                            connectionSettingsStore = connectionSettingsStore,
                            context = context,
                            settings = settings,
                        ),
                    auth =
                        OkHttpProvider.create(
                            authenticator = null,
                            connectionSettingsStore = connectionSettingsStore,
                            context = context,
                            settings = settings,
                        ),
                )
            current = pair
            invalidators.forEach { it() }
            return pair
        }
    }
