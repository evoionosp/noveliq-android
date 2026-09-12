package org.evoionosp.noveliq.domain.session.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.evoionosp.noveliq.domain.auth.SessionRefreshCoordinator
import org.evoionosp.noveliq.domain.auth.TokenRefreshResult
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore

/**
 * Returns the current session with an access token that is expected to still be accepted,
 * refreshing it up front when the stored token is known to have expired.
 *
 * Prefer this over [GetCurrentSessionUseCase] anywhere the session is about to be used for a
 * network call: it saves the wasted 401 round trip, and it is the only path that notices a token
 * has expired while the app was closed. Returns null when there is no session, or when the
 * session turned out to be unrecoverable and has been cleared.
 */
class GetValidSessionUseCase @Inject constructor(
    private val sessionStore: SessionStore,
    private val sessionRefreshCoordinator: SessionRefreshCoordinator
) {
    suspend operator fun invoke(): LoginSession? {
        val session = sessionStore.session.first() ?: return null
        if (!session.isAccessTokenExpired(nowEpochSeconds())) return session

        return when (
            val result = sessionRefreshCoordinator.refresh(staleAccessToken = session.accessToken)
        ) {
            is TokenRefreshResult.Refreshed -> result.session
            TokenRefreshResult.LoggedOut -> null
            // Could not reach the server to refresh. Hand back the expired token rather than
            // failing outright: the call may still be served from cache, and if it does reach the
            // server the 401 authenticator gets another chance to refresh.
            TokenRefreshResult.Retryable -> session
        }
    }

    private fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000
}
