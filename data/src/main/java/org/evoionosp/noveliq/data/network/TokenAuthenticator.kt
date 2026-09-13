package org.evoionosp.noveliq.data.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.evoionosp.noveliq.domain.auth.SessionRefreshCoordinator
import org.evoionosp.noveliq.domain.auth.TokenRefreshResult

/**
 * Recovers from a 401 by rotating the access token once and replaying the request.
 *
 * OkHttp invokes this for any authenticated call that comes back 401, which makes it the one
 * place every API call passes through on the way to an auth failure. Doing the refresh here — as
 * opposed to in each caller — is what stops paths that forgot to handle 401 from silently
 * retrying a dead token forever.
 *
 * Deliberately not installed on the client used for login, server checks, and the refresh call
 * itself, so a rejected login can never recurse back into a refresh.
 */
@Singleton
class TokenAuthenticator
    @Inject
    constructor(
        private val sessionRefreshCoordinator: SessionRefreshCoordinator,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            // Give up rather than loop: if replaying with a fresh token still 401s, the problem is
            // not the token.
            if (priorResponseCount(response) >= MAX_ATTEMPTS) return null

            val staleToken =
                response.request
                    .header(AUTHORIZATION_HEADER)
                    ?.removePrefix(BEARER_PREFIX)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            // OkHttp calls authenticators on its own background threads and expects a blocking
            // answer, so bridging out of coroutines here is the intended shape. The coordinator's
            // mutex means parallel 401s wait on one refresh instead of triggering several.
            val refreshed = runBlocking { sessionRefreshCoordinator.refresh(staleToken) }

            val newToken =
                when (refreshed) {
                    is TokenRefreshResult.Refreshed -> refreshed.session.accessToken

                    // Session is gone, or we could not reach the server. Either way there is nothing to
                    // retry with; let the 401 surface so the caller can report it.
                    TokenRefreshResult.LoggedOut, TokenRefreshResult.Retryable -> return null
                }

            if (newToken.isBlank() || newToken == staleToken) return null

            return response.request
                .newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$newToken")
                .build()
        }

        private fun priorResponseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }

        private companion object {
            const val AUTHORIZATION_HEADER = "Authorization"
            const val BEARER_PREFIX = "Bearer "
            const val MAX_ATTEMPTS = 2
        }
    }
