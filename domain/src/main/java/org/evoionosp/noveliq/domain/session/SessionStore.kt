package org.evoionosp.noveliq.domain.session

import kotlinx.coroutines.flow.Flow

interface SessionStore {
    val session: Flow<LoginSession?>

    /**
     * Base URL of the server most recently signed in to, or null if there has never been one.
     *
     * Outlives [clearSession] on purpose: whether someone logged out deliberately or was pushed
     * back to login by an expired session, they are overwhelmingly likely to sign back in to the
     * same server, so the login form can be prefilled instead of starting blank.
     */
    val lastServerUrl: Flow<String?>

    /**
     * Persists [session] and returns what was actually stored. The store may enrich the session
     * on the way in (for example by deriving the access token expiry), so callers that need the
     * canonical session should use the return value rather than the argument they passed.
     */
    suspend fun saveSession(session: LoginSession): LoginSession

    /** Ends the session. Leaves [lastServerUrl] in place. */
    suspend fun clearSession()
}
