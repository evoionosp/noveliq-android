package org.evoionosp.noveliq.domain.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.evoionosp.noveliq.domain.auth.model.AuthError
import org.evoionosp.noveliq.domain.auth.model.LoginData
import org.evoionosp.noveliq.domain.auth.model.LoginResult
import org.evoionosp.noveliq.domain.auth.repository.AuthRepository
import org.evoionosp.noveliq.domain.session.LoginSession
import org.evoionosp.noveliq.domain.session.SessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRefreshCoordinatorTest {

    @Test
    fun `rotates the access token and persists the new session`() = runTest {
        val store = FakeSessionStore(session(refreshToken = "refresh-1"))
        val authRepository = FakeAuthRepository(
            LoginResult.Success(
                LoginData(accessToken = "new-access", refreshToken = "refresh-2", userId = "user-1")
            )
        )

        val result = SessionRefreshCoordinator(store, authRepository).refresh()

        assertTrue(result is TokenRefreshResult.Refreshed)
        assertEquals("new-access", (result as TokenRefreshResult.Refreshed).session.accessToken)
        assertEquals("new-access", store.session.first()?.accessToken)
        assertEquals("refresh-2", store.session.first()?.refreshToken)
    }

    @Test
    fun `keeps the existing refresh token when the server does not rotate it`() = runTest {
        val store = FakeSessionStore(session(refreshToken = "refresh-1"))
        val authRepository = FakeAuthRepository(
            LoginResult.Success(
                LoginData(accessToken = "new-access", refreshToken = null, userId = null)
            )
        )

        SessionRefreshCoordinator(store, authRepository).refresh()

        assertEquals("refresh-1", store.session.first()?.refreshToken)
    }

    /**
     * The exact shape of the reported bug: a session stored before refresh tokens were persisted
     * has no way back, so it has to end rather than sit there sending a dead token.
     */
    @Test
    fun `logs out and clears the session when there is no refresh token`() = runTest {
        val store = FakeSessionStore(session(refreshToken = null))
        val authRepository = FakeAuthRepository(LoginResult.Success(LoginData("x", "y", "z")))

        val result = SessionRefreshCoordinator(store, authRepository).refresh()

        assertEquals(TokenRefreshResult.LoggedOut, result)
        assertNull(store.session.first())
        assertEquals(0, authRepository.refreshCallCount)
    }

    @Test
    fun `logs out and clears the session when the server rejects the refresh token`() = runTest {
        val store = FakeSessionStore(session(refreshToken = "expired"))
        val authRepository = FakeAuthRepository(LoginResult.Failure(AuthError.HTTP, code = 401))

        val result = SessionRefreshCoordinator(store, authRepository).refresh()

        assertEquals(TokenRefreshResult.LoggedOut, result)
        assertNull(store.session.first())
    }

    @Test
    fun `keeps the session when the refresh cannot reach the server`() = runTest {
        val store = FakeSessionStore(session(refreshToken = "refresh-1"))
        val authRepository = FakeAuthRepository(LoginResult.Failure(AuthError.NETWORK))

        val result = SessionRefreshCoordinator(store, authRepository).refresh()

        assertEquals(TokenRefreshResult.Retryable, result)
        assertEquals("access-1", store.session.first()?.accessToken)
    }

    @Test
    fun `keeps the session when the server fails with a non-auth error`() = runTest {
        val store = FakeSessionStore(session(refreshToken = "refresh-1"))
        val authRepository = FakeAuthRepository(LoginResult.Failure(AuthError.HTTP, code = 503))

        val result = SessionRefreshCoordinator(store, authRepository).refresh()

        assertEquals(TokenRefreshResult.Retryable, result)
        assertEquals("access-1", store.session.first()?.accessToken)
    }

    @Test
    fun `does not refresh again when another caller already rotated the token`() = runTest {
        val store = FakeSessionStore(session(refreshToken = "refresh-1"))
        val authRepository = FakeAuthRepository(
            LoginResult.Success(LoginData("new-access", "refresh-2", "user-1"))
        )
        val coordinator = SessionRefreshCoordinator(store, authRepository)

        coordinator.refresh(staleAccessToken = "access-1")
        // A second caller that failed on the now-replaced token must not burn another refresh.
        val result = coordinator.refresh(staleAccessToken = "access-1")

        assertEquals(1, authRepository.refreshCallCount)
        assertEquals("new-access", (result as TokenRefreshResult.Refreshed).session.accessToken)
    }

    @Test
    fun `logs out when the refreshed session comes back without an access token`() = runTest {
        val store = FakeSessionStore(session(refreshToken = "refresh-1"))
        val authRepository = FakeAuthRepository(
            LoginResult.Success(LoginData(accessToken = "  ", refreshToken = null, userId = null))
        )

        val result = SessionRefreshCoordinator(store, authRepository).refresh()

        assertEquals(TokenRefreshResult.LoggedOut, result)
        assertNull(store.session.first())
    }

    private fun session(refreshToken: String?): LoginSession {
        return LoginSession(
            accessToken = "access-1",
            refreshToken = refreshToken,
            userId = "user-1",
            username = "demo",
            baseUrl = "https://example.com"
        )
    }
}

internal class FakeSessionStore(initial: LoginSession?) : SessionStore {
    private val backing = MutableStateFlow(initial)
    private val lastServer = MutableStateFlow(initial?.baseUrl)

    override val session: Flow<LoginSession?> = backing

    override val lastServerUrl: Flow<String?> = lastServer

    override suspend fun saveSession(session: LoginSession): LoginSession {
        backing.value = session
        lastServer.value = session.baseUrl
        return session
    }

    override suspend fun clearSession() {
        backing.value = null
    }
}

internal class FakeAuthRepository(private val refreshResult: LoginResult) : AuthRepository {
    var refreshCallCount = 0
        private set

    override suspend fun login(
        baseUrl: String,
        username: String,
        password: String
    ): LoginResult = refreshResult

    override suspend fun refreshSession(
        baseUrl: String,
        refreshToken: String
    ): LoginResult {
        refreshCallCount++
        return refreshResult
    }
}
