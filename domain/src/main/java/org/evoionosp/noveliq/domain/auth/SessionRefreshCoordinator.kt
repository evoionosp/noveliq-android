package org.evoionosp.noveliq.domain.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

/** Outcome of an attempt to rotate the current session's access token. */
sealed interface TokenRefreshResult {
    /** A usable session is available. */
    data class Refreshed(val session: LoginSession) : TokenRefreshResult

    /**
     * The session cannot be recovered without a fresh login. The stored session has already been
     * cleared, which is what drives the app back to the auth route.
     */
    data object LoggedOut : TokenRefreshResult

    /**
     * The refresh could not be completed right now — offline, or a server-side problem. The
     * stored session is left intact so the user is not logged out over a temporary failure.
     */
    data object Retryable : TokenRefreshResult
}

/**
 * The single place where the access token is rotated.
 *
 * Auth retry used to be reimplemented by each caller, which meant any path that forgot to handle
 * a 401 (the app-scoped catalog sync) silently hammered the server with a dead token forever.
 * Everything now funnels through here so that:
 *
 * - concurrent 401s collapse into one refresh rather than racing, which matters because
 *   Audiobookshelf rotates the refresh token on use and parallel refreshes invalidate each other;
 * - a definitively rejected session is cleared exactly once, in one place;
 * - a merely unreachable server never logs the user out.
 */
@Singleton
class SessionRefreshCoordinator @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository
) {
    private val mutex = Mutex()

    /**
     * Rotates the access token for the stored session.
     *
     * @param staleAccessToken the token the caller saw fail, when there was one. If the stored
     * token no longer matches it, another caller already refreshed while this one waited for the
     * lock and the new session is returned without spending a second refresh token.
     */
    suspend fun refresh(staleAccessToken: String? = null): TokenRefreshResult = mutex.withLock {
        val current = sessionStore.session.first()
            ?: return@withLock TokenRefreshResult.LoggedOut

        if (staleAccessToken != null && current.accessToken != staleAccessToken) {
            return@withLock TokenRefreshResult.Refreshed(current)
        }

        val refreshToken = current.refreshToken?.takeIf { it.isNotBlank() }
            ?: return@withLock logOut()

        when (val result = authRepository.refreshSession(current.baseUrl, refreshToken)) {
            is LoginResult.Success -> persistRotatedSession(current, result)
            is LoginResult.Failure ->
                if (isUnrecoverable(result)) {
                    logOut()
                } else {
                    // Offline, timeout, 5xx: keep the session and let the next attempt try again.
                    TokenRefreshResult.Retryable
                }
        }
    }

    /**
     * Whether [failure] means the refresh token will never work again, as opposed to the server
     * merely being unreachable right now. Only the former justifies ending the session — logging
     * someone out because their train went into a tunnel would be worse than the bug this fixes.
     */
    private fun isUnrecoverable(failure: LoginResult.Failure): Boolean {
        if (failure.error == AuthError.INVALID_BASE_URL) return true
        if (failure.error != AuthError.HTTP) return false
        val code = failure.code ?: return false
        return UNRECOVERABLE_HTTP_CODES.contains(code)
    }

    private suspend fun persistRotatedSession(
        current: LoginSession,
        result: LoginResult.Success
    ): TokenRefreshResult {
        val accessToken = result.data.accessToken?.trim().orEmpty()
        if (accessToken.isBlank()) return logOut()

        val rotated = current.copy(
            accessToken = accessToken,
            // Audiobookshelf only returns a new refresh token when it rotates one; keep the
            // existing token otherwise so the session does not lose its ability to refresh.
            refreshToken = result.data.refreshToken?.trim()?.takeIf { it.isNotBlank() }
                ?: current.refreshToken,
            userId = result.data.userId?.trim() ?: current.userId,
            // Recomputed by the session store from the new token.
            accessTokenExpiresAtEpochSeconds = null
        )
        return TokenRefreshResult.Refreshed(sessionStore.saveSession(rotated))
    }

    private suspend fun logOut(): TokenRefreshResult {
        sessionStore.clearSession()
        return TokenRefreshResult.LoggedOut
    }

    private companion object {
        val UNRECOVERABLE_HTTP_CODES = setOf(400, 401, 403, 404)
    }
}
